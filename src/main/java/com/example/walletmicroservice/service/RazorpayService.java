package com.example.walletmicroservice.service;

import com.example.walletmicroservice.dto.OrderRequestDTO;
import com.example.walletmicroservice.dto.OrderResponseDTO;
import com.example.walletmicroservice.dto.RefundRequestDTO;
import com.example.walletmicroservice.entity.PaymentTransaction;
import com.example.walletmicroservice.entity.GeneralLedger;
import com.example.walletmicroservice.entity.ChartOfAccounts;
import com.example.walletmicroservice.repository.PaymentTransactionRepository;
import com.example.walletmicroservice.repository.GeneralLedgerRepository;
import com.example.walletmicroservice.repository.ChartOfAccountsRepository;
import com.razorpay.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    /**
     * API-1: Create Razorpay Order
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request) throws RazorpayException {
        // Generate INTERNAL transaction ID (system's ID)
        String internalTransactionId = "TRN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        // Generate INTERNAL order ID if not provided
        String internalOrderId = request.getOrderId();
        if (internalOrderId == null || internalOrderId.isEmpty()) {
            internalOrderId = "ORD" + System.currentTimeMillis() + "_" + request.getCustomerId();
        }

        // Create Razorpay Order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", internalOrderId); // Internal order ID as receipt

        if (request.getPaymentMethod() != null) {
            JSONObject notes = new JSONObject();
            notes.put("payment_method", request.getPaymentMethod());
            notes.put("internal_order_id", internalOrderId);
            notes.put("customer_id", request.getCustomerId());
            orderRequest.put("notes", notes);
        }

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        // Get RAZORPAY's order ID
        String razorpayOrderId = razorpayOrder.get("id");

        // Save to payment_transactions table - BOTH IDs
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setTransactionId(internalTransactionId); // internal ID
        transaction.setRazorpayOrderId(razorpayOrderId); // Razorpay's order ID
        transaction.setCustomerId(request.getCustomerId());
        transaction.setOrderId(internalOrderId); // Your internal order ID
        transaction.setTransactionType(PaymentTransaction.TransactionType.PAYIN);
        transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.REAL);
        transaction.setAmount(request.getAmount());
        transaction.setStatus(PaymentTransaction.TransactionStatus.CREATED);
        transaction.setCurrency(request.getCurrency());
        // Store BOTH request and response in metadata
        JSONObject metadata = new JSONObject();
        metadata.put("request", new JSONObject(request));
        metadata.put("razorpay_order_response", razorpayOrder.toString());
        metadata.put("internal_order_id", internalOrderId);
        metadata.put("razorpay_order_id", razorpayOrderId);
        transaction.setMetadata(metadata.toString());

        paymentTransactionRepository.save(transaction);
        // Prepare response - include BOTH IDs
        OrderResponseDTO response = new OrderResponseDTO();
        response.setRazorpayOrderId(razorpayOrderId); // Razorpay's ID
        response.setTransactionId(internalTransactionId); // internal transaction ID
        response.setAmount(request.getAmount());
        response.setCurrency(request.getCurrency());
        response.setCustomerId(request.getCustomerId());
        response.setOrderId(internalOrderId); // internal order ID
        response.setKeyId(razorpayKeyId);
        response.setStatus("created");
        response.setCreatedAt(razorpayOrder.get("created_at").toString());

        log.info("Order created: InternalOrderId={}, RazorpayOrderId={}, Customer={}",
                internalOrderId, razorpayOrderId, request.getCustomerId());

        return response;
    }

    /**
     * Get Payment Transaction by ID
     */
    public PaymentTransaction getPaymentTransaction(String transactionId) {
        return paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + transactionId));
    }

    /**
     * Verify Payment Signature (for QR code/UPI payments)
     */
    // 1️⃣ Verify signature only
    public boolean verifySignature(String paymentId, String orderId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key =
                    new SecretKeySpec(razorpayKeySecret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            String generated = Hex.encodeHexString(hash);

            return generated.equals(signature.trim());
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    public String getPaymentStatus(String paymentId, String orderId) {
        try {
            Payment payment = razorpayClient.payments.fetch(paymentId);

            String status = payment.get("status").toString(); // captured / authorized / failed
            String method = payment.get("method").toString().toUpperCase();

            List<PaymentTransaction> transactions = paymentTransactionRepository.findAllByRazorpayOrderId(orderId);

            for (PaymentTransaction transaction : transactions) {
                transaction.setRazorpayPaymentId(paymentId);
                transaction.setStatus(PaymentTransaction.TransactionStatus.valueOf(status.toUpperCase()));
                transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.valueOf(method.toUpperCase()));
                paymentTransactionRepository.save(transaction);
            }

            return switch (status) {
                case "created" -> "PENDING";
                case "captured" -> "Payment SUCCESS";
                case "authorized" -> "Payment AUTHORIZED (not yet captured)";
                case "failed" -> "Payment FAILED";
                default -> "Payment PENDING";
            };

        } catch (Exception e) {
            log.error("Fetching payment status failed", e);
            return "Error fetching payment status";
        }
    }


    /**
     * API-3: Handle Razorpay Webhook
     */
    @Transactional
    public void handleWebhook(JSONObject webhookPayload) {
        String event = webhookPayload.getString("event");
        String entity = webhookPayload.getString("entity");

        log.info("Received webhook event: {} for entity: {}", event, entity);

        switch (event) {
            case "payment.captured":
                handlePaymentCaptured(webhookPayload);
                break;
            case "payment.failed":
                handlePaymentFailed(webhookPayload);
                break;
            case "refund.created":
                handleRefundCreated(webhookPayload);
                break;
            case "refund.processed":
                handleRefundProcessed(webhookPayload);
                break;
//            case "payout.processed":
//                handlePayoutProcessed(webhookPayload);
//                break;
//            case "payout.failed":
//                handlePayoutFailed(webhookPayload);
//                break;
            default:
                log.warn("Unhandled webhook event: {}", event);
        }
    }

    private void handlePaymentCaptured(JSONObject webhookPayload) {
        JSONObject payment = webhookPayload.getJSONObject("payload").getJSONObject("payment");
        String razorpayPaymentId = payment.getString("id");
        String razorpayOrderId = payment.getString("order_id");

        // Find the transaction
        PaymentTransaction transaction = (PaymentTransaction) paymentTransactionRepository
                .findAllByRazorpayOrderId(razorpayOrderId);
        // Update transaction
        transaction.setRazorpayPaymentId(razorpayPaymentId);
        transaction.setStatus(PaymentTransaction.TransactionStatus.CAPTURED);

        String method = payment.optString("method", "unknown").toUpperCase();
        try {
            transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.valueOf(method));
        } catch (IllegalArgumentException e) {
            transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.REAL);
        }

        transaction.setMetadata(new JSONObject(transaction.getMetadata() != null ? transaction.getMetadata() : "{}")
                .put("webhook_payload", webhookPayload.toString())
                .toString());

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        // Create General Ledger entries (Double Entry) - NO ManyToOne
        createPayInLedgerEntries(savedTransaction, method);

        log.info("Payment captured for order: {}, payment: {}", razorpayOrderId, razorpayPaymentId);
    }

    /**
     * Create PAYIN ledger entries (Payment captured)
     */
    private void createPayInLedgerEntries(PaymentTransaction transaction, String paymentMethod) {
        // Get account details from master table
        ChartOfAccounts companyBankAccount = chartOfAccountsRepository.findById("Company Bank A/C")
                .orElseThrow(() -> new RuntimeException("Account not found: Company Bank A/C"));

        ChartOfAccounts salesRevenueAccount = chartOfAccountsRepository.findById("Sales Revenue")
                .orElseThrow(() -> new RuntimeException("Account not found: Sales Revenue"));

        String ledgerRef = "PAYIN-" + System.currentTimeMillis();

        // Create Debit Entry (Company Bank A/C)
        GeneralLedger debitEntry = createLedgerEntry(
                transaction.getId().toString(),
                transaction.getTransactionId(),
                ledgerRef,
                transaction.getCustomerId(),
                transaction.getOrderId(),
                transaction.getAmount(),
                "Payment received via " + paymentMethod + " for order: " + transaction.getOrderId(),
                companyBankAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Create Credit Entry (Sales Revenue)
        GeneralLedger creditEntry = createLedgerEntry(
                transaction.getId().toString(),
                transaction.getTransactionId(),
                ledgerRef,
                transaction.getCustomerId(),
                transaction.getOrderId(),
                transaction.getAmount(),
                "Sales revenue from order: " + transaction.getOrderId(),
                salesRevenueAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created PAYIN ledger entries for transaction: {}", transaction.getTransactionId());
    }

    /**
     * Helper method to create ledger entry with copied account values
     */
    private GeneralLedger createLedgerEntry(String paymentTransactionId,
                                            String transactionId,
                                            String ledgerRef,
                                            String customerId,
                                            String orderId,
                                            BigDecimal amount,
                                            String description,
                                            ChartOfAccounts account,
                                            GeneralLedger.EntryType entryType) {

        GeneralLedger entry = new GeneralLedger();
        entry.setLedgerEntryId(generateLedgerEntryId(entryType));
        entry.setPaymentTransactionId(paymentTransactionId);
        entry.setTransactionId(transactionId);
        entry.setEntryDate(LocalDate.now());
        entry.setCustomerId(customerId);
        entry.setOrderId(orderId);

        // Copy ALL values from ChartOfAccounts (no relationship)
        entry.setAccountId(account.getAccountId());
        entry.setAccountName(account.getAccountName());
        entry.setAccountType(account.getAccountType().name());
        entry.setNormalBalance(account.getNormalBalance().name());
        entry.setLedgerType(account.getLedgerType().name());
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setDescription(description);
        entry.setIsReversed(false);

        return entry;
    }

    private String generateLedgerEntryId(GeneralLedger.EntryType entryType) {
        String prefix = entryType == GeneralLedger.EntryType.DEBIT ? "DEB" : "CRED";
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void handlePaymentFailed(JSONObject webhookPayload) {
        JSONObject payment = webhookPayload.getJSONObject("payload").getJSONObject("payment");
        String razorpayPaymentId = payment.getString("id");
        String razorpayOrderId = payment.getString("order_id");

        PaymentTransaction transaction = (PaymentTransaction) paymentTransactionRepository
                .findAllByRazorpayOrderId(razorpayOrderId);

        transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
        transaction.setMetadata(new JSONObject(transaction.getMetadata() != null ? transaction.getMetadata() : "{}")
                .put("webhook_payload", webhookPayload.toString())
                .toString());

        paymentTransactionRepository.save(transaction);

        log.warn("Payment failed for order: {}, payment: {}", razorpayOrderId, razorpayPaymentId);
    }

    /**
     * API-4: Create Refund
     */
    @Transactional
    public PaymentTransaction createRefund(RefundRequestDTO request) throws RazorpayException {
        // Find the original payment transaction
        PaymentTransaction originalTransaction = paymentTransactionRepository
                .findByTransactionId(request.getPaymentTransactionId())
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + request.getPaymentTransactionId()));

        // Check if refund is possible
        if (!originalTransaction.getStatus().equals(PaymentTransaction.TransactionStatus.CAPTURED)) {
            throw new RuntimeException("Cannot refund unsuccessful payment");
        }

        if (originalTransaction.getRefundStatus() != null &&
                originalTransaction.getRefundStatus().equals(PaymentTransaction.RefundStatus.PROCESSED)) {
            throw new RuntimeException("Refund already processed");
        }

        // Create Razorpay refund
        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        refundRequest.put("speed", "normal");

        if (request.getReason() != null) {
            refundRequest.put("receipt", "Refund for: " + originalTransaction.getOrderId());
        }

        if (request.getNotes() != null) {
            refundRequest.put("notes", new JSONObject(request.getNotes()));
        }

        Refund razorpayRefund = razorpayClient.payments.refund(
                originalTransaction.getRazorpayPaymentId(),
                refundRequest
        );

        // Create new transaction for refund
        String refundTransactionId = "RFND" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        PaymentTransaction refundTransaction = new PaymentTransaction();
        refundTransaction.setTransactionId(refundTransactionId);
        refundTransaction.setRazorpayPaymentId(originalTransaction.getRazorpayPaymentId());
        refundTransaction.setRazorpayRefundId(razorpayRefund.get("id"));
        refundTransaction.setCustomerId(originalTransaction.getCustomerId());
        refundTransaction.setOrderId(originalTransaction.getOrderId());
        refundTransaction.setTransactionType(PaymentTransaction.TransactionType.REFUND);
        refundTransaction.setPaymentMethod(originalTransaction.getPaymentMethod());
        refundTransaction.setAmount(request.getAmount());
        refundTransaction.setCurrency(originalTransaction.getCurrency());
        refundTransaction.setStatus(PaymentTransaction.TransactionStatus.AUTHORIZED);
        refundTransaction.setRefundStatus(PaymentTransaction.RefundStatus.PROCESSED);
        refundTransaction.setMetadata(new JSONObject()
                .put("razorpay_refund_response", razorpayRefund.toJson())
                .put("refund_reason", request.getReason())
                .toString());

        PaymentTransaction savedRefundTransaction = paymentTransactionRepository.save(refundTransaction);

        // Update original transaction
        originalTransaction.setRefundStatus(PaymentTransaction.RefundStatus.PROCESSED);
        originalTransaction.setRefundApprovedAt(java.time.LocalDateTime.now());
        paymentTransactionRepository.save(originalTransaction);

        // Create General Ledger entries for refund - NO ManyToOne
        createRefundLedgerEntries(savedRefundTransaction, originalTransaction);

        log.info("Refund created: {} for payment: {}", razorpayRefund.get("id"), originalTransaction.getRazorpayPaymentId());

        return savedRefundTransaction;
    }

    /**
     * Create REFUND ledger entries
     */
    private void createRefundLedgerEntries(PaymentTransaction refundTransaction,
                                           PaymentTransaction originalTransaction) {

        ChartOfAccounts salesRevenueAccount = chartOfAccountsRepository.findById("Sales Revenue")
                .orElseThrow(() -> new RuntimeException("Account not found: Sales Revenue"));

        ChartOfAccounts refundPayableAccount = chartOfAccountsRepository.findById("Customer Refund Payable")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Refund Payable"));

        String ledgerRef = "REFUND-" + System.currentTimeMillis();

        // Debit Sales Revenue (Reversal)
        GeneralLedger debitEntry = createLedgerEntry(
                refundTransaction.getId().toString(),
                refundTransaction.getTransactionId(),
                ledgerRef,
                refundTransaction.getCustomerId(),
                refundTransaction.getOrderId(),
                refundTransaction.getAmount(),
                "Refund issued for order: " + refundTransaction.getOrderId() +
                        " (Original TXN: " + originalTransaction.getTransactionId() + ")",
                salesRevenueAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Customer Refund Payable
        GeneralLedger creditEntry = createLedgerEntry(
                refundTransaction.getId().toString(),
                refundTransaction.getTransactionId(),
                ledgerRef,
                refundTransaction.getCustomerId(),
                refundTransaction.getOrderId(),
                refundTransaction.getAmount(),
                "Refund payable to customer for order: " + refundTransaction.getOrderId(),
                refundPayableAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created REFUND ledger entries for transaction: {}", refundTransaction.getTransactionId());
    }

    private void handleRefundCreated(JSONObject webhookPayload) {
        JSONObject refund = webhookPayload.getJSONObject("payload").getJSONObject("refund");
        String razorpayRefundId = refund.getString("id");
        String razorpayPaymentId = refund.getString("payment_id");

        log.info("Refund created: {} for payment: {}", razorpayRefundId, razorpayPaymentId);
    }

    private void handleRefundProcessed(JSONObject webhookPayload) {
        JSONObject refund = webhookPayload.getJSONObject("payload").getJSONObject("refund");
        String razorpayRefundId = refund.getString("id");
        String razorpayPaymentId = refund.getString("payment_id");

        // Find refund transaction
        PaymentTransaction refundTransaction = paymentTransactionRepository
                .findByRazorpayRefundId(razorpayRefundId)
                .orElseThrow(() -> new RuntimeException("Refund transaction not found: " + razorpayRefundId));

        // Update status
        refundTransaction.setStatus(PaymentTransaction.TransactionStatus.AUTHORIZED);
        refundTransaction.setMetadata(new JSONObject(refundTransaction.getMetadata() != null ? refundTransaction.getMetadata() : "{}")
                .put("webhook_payload", webhookPayload.toString())
                .toString());

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(refundTransaction);

        // Create refund payout GL entries - NO ManyToOne
        createRefundPayoutLedgerEntries(savedTransaction);

        log.info("Refund processed: {} for payment: {}", razorpayRefundId, razorpayPaymentId);
    }

    /**
     * Create REFUND PAYOUT ledger entries (when refund is processed/bank transfer)
     */
    private void createRefundPayoutLedgerEntries(PaymentTransaction refundTransaction) {

        ChartOfAccounts refundPayableAccount = chartOfAccountsRepository.findById("Customer Refund Payable")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Refund Payable"));

        ChartOfAccounts companyBankAccount = chartOfAccountsRepository.findById("Company Bank A/C")
                .orElseThrow(() -> new RuntimeException("Account not found: Company Bank A/C"));

        String ledgerRef = "REFUND-PAYOUT-" + System.currentTimeMillis();

        // Debit Customer Refund Payable
        GeneralLedger debitEntry = createLedgerEntry(
                refundTransaction.getId().toString(),
                refundTransaction.getTransactionId(),
                ledgerRef,
                refundTransaction.getCustomerId(),
                refundTransaction.getOrderId(),
                refundTransaction.getAmount(),
                "Refund payout to customer for order: " + refundTransaction.getOrderId(),
                refundPayableAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Company Bank A/C
        GeneralLedger creditEntry = createLedgerEntry(
                refundTransaction.getId().toString(),
                refundTransaction.getTransactionId(),
                ledgerRef,
                refundTransaction.getCustomerId(),
                refundTransaction.getOrderId(),
                refundTransaction.getAmount(),
                "Bank transfer for refund of order: " + refundTransaction.getOrderId(),
                companyBankAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created REFUND PAYOUT ledger entries for transaction: {}", refundTransaction.getTransactionId());
    }

//    private void handlePayoutProcessed(JSONObject webhookPayload) {
//        JSONObject payout = webhookPayload.getJSONObject("payload").getJSONObject("payout");
//        String razorpayPayoutId = payout.getString("id");
//
//        log.info("Payout processed: {}", razorpayPayoutId);
//
//        // Find wallet withdrawal transaction
//        paymentTransactionRepository.findByRazorpayPayoutId(razorpayPayoutId)
//                .ifPresent(transaction -> {
//                    transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
//                    PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
//
//                    // Create GL entries for successful payout - NO ManyToOne
//                    createPayoutProcessedLedgerEntries(savedTransaction);
//                });
//    }

    /**
     * Create PAYOUT PROCESSED ledger entries (wallet withdrawal bank transfer)
     */
    private void createPayoutProcessedLedgerEntries(PaymentTransaction payoutTransaction) {

        ChartOfAccounts payoutClearingAccount = chartOfAccountsRepository.findById("Payout Pending / Clearing")
                .orElseThrow(() -> new RuntimeException("Account not found: Payout Pending / Clearing"));

        ChartOfAccounts companyBankAccount = chartOfAccountsRepository.findById("Company Bank A/C")
                .orElseThrow(() -> new RuntimeException("Account not found: Company Bank A/C"));

        String ledgerRef = "PAYOUT-" + System.currentTimeMillis();

        // Debit Payout Pending / Clearing
        GeneralLedger debitEntry = createLedgerEntry(
                payoutTransaction.getId().toString(),
                payoutTransaction.getTransactionId(),
                ledgerRef,
                payoutTransaction.getCustomerId(),
                payoutTransaction.getOrderId(),
                payoutTransaction.getAmount(),
                "Payout processed to customer bank for withdrawal",
                payoutClearingAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Company Bank A/C
        GeneralLedger creditEntry = createLedgerEntry(
                payoutTransaction.getId().toString(),
                payoutTransaction.getTransactionId(),
                ledgerRef,
                payoutTransaction.getCustomerId(),
                payoutTransaction.getOrderId(),
                payoutTransaction.getAmount(),
                "Bank transfer for customer withdrawal",
                companyBankAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created PAYOUT PROCESSED ledger entries for transaction: {}", payoutTransaction.getTransactionId());
    }

//    private void handlePayoutFailed(JSONObject webhookPayload) {
//        JSONObject payout = webhookPayload.getJSONObject("payload").getJSONObject("payout");
//        String razorpayPayoutId = payout.getString("id");
//        String failureReason = payout.optString("failure_reason", "Unknown");
//
//        log.error("Payout failed: {}, reason: {}", razorpayPayoutId, failureReason);
//
//        // Update wallet withdrawal transaction if exists
//        paymentTransactionRepository.findByRazorpayPayoutId(razorpayPayoutId)
//                .ifPresent(transaction -> {
//                    transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
//                    transaction.setMetadata(new JSONObject(transaction.getMetadata() != null ? transaction.getMetadata() : "{}")
//                            .put("failure_reason", failureReason)
//                            .toString());
//                    paymentTransactionRepository.save(transaction);
//                });
//    }

    /**
     * Get payment details from Razorpay
     */
    public JSONObject getPaymentDetails(String razorpayPaymentId) throws RazorpayException {
        return razorpayClient.payments.fetch(razorpayPaymentId).toJson();
    }

    /**
     * Get order details from Razorpay
     */
    public JSONObject getOrderDetails(String razorpayOrderId) throws RazorpayException {
        return razorpayClient.orders.fetch(razorpayOrderId).toJson();
    }

    /**
     * Additional helper methods for wallet operations
     */

    /**
     * Create wallet transfer transaction
     */
    @Transactional
    public PaymentTransaction createWalletTransfer(String customerId, String orderId, BigDecimal amount, String description) {
        String transactionId = "WALLET" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setCustomerId(customerId);
        transaction.setOrderId(orderId);
        transaction.setTransactionType(PaymentTransaction.TransactionType.WALLET_TRANSFER);
        transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.WALLET);
        transaction.setAmount(amount);
        transaction.setCurrency("INR");
        transaction.setStatus(PaymentTransaction.TransactionStatus.CAPTURED);
        transaction.setIsWalletTransfer(true);
        transaction.setMetadata(new JSONObject()
                .put("description", description)
                .toString());

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        // Create GL entries for wallet transfer - NO ManyToOne
        createWalletTransferLedgerEntries(savedTransaction, description);

        return savedTransaction;
    }

    /**
     * Create WALLET TRANSFER ledger entries
     */
    private void createWalletTransferLedgerEntries(PaymentTransaction transaction, String description) {

        ChartOfAccounts payoutExpensesAccount = chartOfAccountsRepository.findById("Customer Payout Expenses")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Payout Expenses"));

        ChartOfAccounts walletLiabilityAccount = chartOfAccountsRepository.findById("Customer Wallet Liability")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Wallet Liability"));

        String ledgerRef = "WALLET-TRANSFER-" + System.currentTimeMillis();

        // Debit Customer Payout Expenses
        GeneralLedger debitEntry = createLedgerEntry(
                transaction.getId().toString(),
                transaction.getTransactionId(),
                ledgerRef,
                transaction.getCustomerId(),
                transaction.getOrderId(),
                transaction.getAmount(),
                description,
                payoutExpensesAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Customer Wallet Liability
        GeneralLedger creditEntry = createLedgerEntry(
                transaction.getId().toString(),
                transaction.getTransactionId(),
                ledgerRef,
                transaction.getCustomerId(),
                transaction.getOrderId(),
                transaction.getAmount(),
                description,
                walletLiabilityAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created WALLET TRANSFER ledger entries for transaction: {}", transaction.getTransactionId());
    }

    /**
     * Create WALLET PAYOUT ledger entries (wallet withdrawal initiated)
     */
    @Transactional
    public PaymentTransaction createWalletPayout(String customerId, String orderId, BigDecimal amount, String description) {
        String transactionId = "WPOUT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setCustomerId(customerId);
        transaction.setOrderId(orderId);
        transaction.setTransactionType(PaymentTransaction.TransactionType.PAYOUT);
        transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.WALLET);
        transaction.setAmount(amount);
        transaction.setCurrency("INR");
        transaction.setStatus(PaymentTransaction.TransactionStatus.AUTHORIZED);
        transaction.setIsWalletTransfer(true);
        transaction.setMetadata(new JSONObject()
                .put("description", description)
                .put("type", "WALLET_WITHDRAWAL")
                .toString());

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        // Create wallet payout GL entries
        createWalletPayoutInitiatedLedgerEntries(savedTransaction, description);

        return savedTransaction;
    }

    /**
     * Create WALLET PAYOUT INITIATED ledger entries
     */
    private void createWalletPayoutInitiatedLedgerEntries(PaymentTransaction transaction, String description) {

        ChartOfAccounts walletLiabilityAccount = chartOfAccountsRepository.findById("Customer Wallet Liability")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Wallet Liability"));

        ChartOfAccounts payoutClearingAccount = chartOfAccountsRepository.findById("Payout Pending / Clearing")
                .orElseThrow(() -> new RuntimeException("Account not found: Payout Pending / Clearing"));

        String ledgerRef = "WALLET-PAYOUT-" + System.currentTimeMillis();

        // Debit Customer Wallet Liability
        GeneralLedger debitEntry = createLedgerEntry(
                transaction.getId().toString(),
                transaction.getTransactionId(),
                ledgerRef,
                transaction.getCustomerId(),
                transaction.getOrderId(),
                transaction.getAmount(),
                "Wallet withdrawal initiated: " + description,
                walletLiabilityAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Payout Pending / Clearing
        GeneralLedger creditEntry = createLedgerEntry(
                transaction.getId().toString(),
                transaction.getTransactionId(),
                ledgerRef,
                transaction.getCustomerId(),
                transaction.getOrderId(),
                transaction.getAmount(),
                "Pending payout to customer bank: " + description,
                payoutClearingAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created WALLET PAYOUT INITIATED ledger entries for transaction: {}", transaction.getTransactionId());
    }

    /**
     * Calculate wallet balance for a customer
     */
    public BigDecimal getWalletBalance(String customerId) {
        try {
            // Get total credits to wallet (customer wallet liability increases)
            BigDecimal totalCredits = generalLedgerRepository.sumWalletCredits(customerId,
                    "Customer Wallet Liability", GeneralLedger.EntryType.CREDIT);

            // Get total debits from wallet (customer wallet liability decreases)
            BigDecimal totalDebits = generalLedgerRepository.sumWalletDebits(customerId,
                    "Customer Wallet Liability", GeneralLedger.EntryType.DEBIT);

            if (totalCredits == null) totalCredits = BigDecimal.ZERO;
            if (totalDebits == null) totalDebits = BigDecimal.ZERO;

            return totalCredits.subtract(totalDebits);
        } catch (Exception e) {
            log.error("Error calculating wallet balance for customer: {}", customerId, e);
            return BigDecimal.ZERO;
        }
    }
}