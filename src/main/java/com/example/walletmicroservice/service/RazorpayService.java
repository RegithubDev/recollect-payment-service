package com.example.walletmicroservice.service;

import com.example.walletmicroservice.dto.*;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.walletmicroservice.util.SecurityUtil.getCurrentUserId;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final LedgerService ledgerService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ================================
    // API-1: CREATE ORDER
    // ================================
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request) throws RazorpayException {
        log.info("Creating order for customer: {}, amount: {}", request.getCustomerId(), request.getAmount());

        // Generate internal IDs
        String internalTransactionId = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        String internalOrderId = request.getOrderId() != null ? request.getOrderId() :
                "ORD" + System.currentTimeMillis() + "_" + request.getCustomerId();

        // Create Razorpay Order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", internalOrderId);

        // Add notes for metadata
        JSONObject notes = new JSONObject();
        notes.put("internal_order_id", internalOrderId);
        notes.put("customer_id", request.getCustomerId());
        notes.put("payment_method", request.getPaymentMethod());
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getNotes().entrySet()) {
                notes.put(entry.getKey(), entry.getValue());
            }
        }

        orderRequest.put("notes", notes);

        // Call Razorpay API
        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        // Create PaymentTransaction entity
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(internalTransactionId);
        transaction.setCustomerId(request.getCustomerId());
        transaction.setOrderId(internalOrderId);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setCreatedUid(getCurrentUserId());
        transaction.setUpdatedUid(getCurrentUserId());

        // Use the setOrderCreateFields method from entity
        JSONObject razorpayResponse = new JSONObject(razorpayOrder.toString());
        transaction.setOrderCreateFields(
                internalTransactionId,
                request.getCustomerId(),
                internalOrderId,
                request.getAmount(),
                razorpayOrderId,
                razorpayResponse
        );

        // Save transaction
        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        // Create response DTO
        OrderResponseDTO response = new OrderResponseDTO();
        response.setRazorpayOrderId(razorpayOrderId);
        response.setTransactionId(internalTransactionId);
        response.setOrderId(internalOrderId);
        response.setCustomerId(request.getCustomerId());
        response.setAmount(request.getAmount());
        response.setCurrency(request.getCurrency());
        response.setKeyId(razorpayKeyId);
        response.setStatus("created");
        response.setCreatedAt(String.valueOf(LocalDateTime.now()));

        log.info("Order created successfully: TransactionId={}, RazorpayOrderId={}",
                internalTransactionId, razorpayOrderId);

        return response;
    }

    // ================================
    // API-2: GET PAYMENT TRANSACTION
    // ================================
    public PaymentTransaction getPaymentTransaction(String transactionId) {
        return paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + transactionId));
    }

    // ================================
    // API-3: VERIFY PAYMENT SIGNATURE
    // ================================
    public boolean verifySignature(String paymentId, String orderId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(razorpayKeySecret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            String generated = Hex.encodeHexString(hash);
            return generated.equals(signature.trim());

        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    // ================================
    // API-4: GET PAYMENT STATUS
    // ================================
    public String getPaymentStatus(String paymentId, String orderId) {
        try {
            Payment payment = razorpayClient.payments.fetch(paymentId);
            String status = payment.get("status").toString();
            String method = payment.get("method").toString().toUpperCase();

            // Update transaction status
            List<PaymentTransaction> transactions = paymentTransactionRepository.findAllByRazorpayOrderId(orderId);
            for (PaymentTransaction transaction : transactions) {
                transaction.setRazorpayPaymentId(paymentId);
                transaction.setStatus(PaymentTransaction.TransactionStatus.valueOf(status.toUpperCase()));
                transaction.setPaymentMethod(PaymentTransaction.PaymentMethod.valueOf(method));

                // Add payment verification metadata
                JSONObject verificationDetails = new JSONObject();
                verificationDetails.put("payment_id", paymentId);
                verificationDetails.put("method", method);
                verificationDetails.put("status", status);
                verificationDetails.put("verified_at", LocalDateTime.now().toString());

                transaction.setPaymentVerificationFields(paymentId, true, verificationDetails);

                paymentTransactionRepository.save(transaction);
                ledgerService.recordPaymentSuccess(paymentId, transaction.getTransactionId(),transaction.getCustomerId(), orderId, transaction.getAmount(), getCurrentUserId());
            }

            return switch (status) {
                case "captured" -> "PAYMENT_SUCCESS";
                case "authorized" -> "PAYMENT_AUTHORIZED";
                case "failed" -> "PAYMENT_FAILED";
                default -> "PAYMENT_PENDING";
            };

        } catch (Exception e) {
            log.error("Fetching payment status failed", e);
            return "ERROR_FETCHING_STATUS";
        }
    }

    // ================================
    // API-5: HANDLE WEBHOOK
    // ================================
    @Transactional
    public void handleWebhook(JSONObject webhookPayload) {
        String event = webhookPayload.getString("event");
        log.info("Processing webhook event: {}", event);

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
            default:
                log.warn("Unhandled webhook event: {}", event);
        }
    }

    private void handlePaymentCaptured(JSONObject webhookPayload) {
        JSONObject payment = webhookPayload.getJSONObject("payload").getJSONObject("payment");
        String razorpayPaymentId = payment.getString("id");
        String razorpayOrderId = payment.getString("order_id");

        // Find transaction by razorpay_order_id
        List<PaymentTransaction> transactions = paymentTransactionRepository.findAllByRazorpayOrderId(razorpayOrderId);

        for (PaymentTransaction transaction : transactions) {
            String method = payment.optString("method", "unknown").toUpperCase();
            JSONObject paymentDetails = new JSONObject(payment.toString());

            // Use the setPaymentCaptureFields method
            transaction.setPaymentCaptureFields(
                    razorpayPaymentId,
                    method,
                    paymentDetails,
                    webhookPayload
            );

            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

            // Create General Ledger entries

            log.info("Payment captured: OrderId={}, PaymentId={}", razorpayOrderId, razorpayPaymentId);
        }
    }

    private void handlePaymentFailed(JSONObject webhookPayload) {
        JSONObject payment = webhookPayload.getJSONObject("payload").getJSONObject("payment");
        String razorpayPaymentId = payment.getString("id");
        String razorpayOrderId = payment.getString("order_id");

        List<PaymentTransaction> transactions = paymentTransactionRepository.findAllByRazorpayOrderId(razorpayOrderId);

        for (PaymentTransaction transaction : transactions) {
            JSONObject failureDetails = new JSONObject();
            failureDetails.put("error_code", payment.optString("error_code"));
            failureDetails.put("error_description", payment.optString("error_description"));
            failureDetails.put("error_source", payment.optString("error_source"));

            transaction.setPaymentFailureFields(
                    payment.optString("error_description", "Payment failed"),
                    failureDetails
            );

            paymentTransactionRepository.save(transaction);
            log.warn("Payment failed: OrderId={}, PaymentId={}", razorpayOrderId, razorpayPaymentId);
        }
    }

    private void validateRefundRequest(PaymentTransaction originalTransaction, RefundRequestDTO request) {
        // Check if payment was successful
        if (!originalTransaction.getStatus().equals(PaymentTransaction.TransactionStatus.CAPTURED)) {
            throw new RuntimeException("Cannot refund unsuccessful payment. Payment status: " +
                    originalTransaction.getStatus());
        }

        // Check if already refunded
        if (originalTransaction.getRefundStatus() != null &&
                originalTransaction.getRefundStatus().equals(PaymentTransaction.RefundStatus.PROCESSED)) {
            throw new RuntimeException("Refund already processed for this payment");
        }

        // Check refund amount
        if (request.getAmount().compareTo(originalTransaction.getAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot be greater than original payment amount");
        }

        // Check if partial refund is allowed
        if (request.getAmount().compareTo(originalTransaction.getAmount()) < 0 &&
                !Boolean.TRUE.equals(request.getIsPartialRefund())) {
            throw new RuntimeException("Partial refund not allowed. Set isPartialRefund to true");
        }
    }

    public PaymentTransaction processRazorpayRefund(RefundRequestDTO request) throws RazorpayException {
        // Find original payment transaction
        PaymentTransaction originalTransaction = paymentTransactionRepository
                .findByTransactionId(request.getPaymentTransactionId())
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + request.getPaymentTransactionId()));

        // Validate refund
        validateRefundRequest(originalTransaction, request);

        // Create Razorpay refund
        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        refundRequest.put("speed", "normal");
        refundRequest.put("receipt", "Refund-" + request.getPaymentTransactionId());

        Refund razorpayRefund = razorpayClient.payments.refund(
                originalTransaction.getRazorpayPaymentId(),
                refundRequest
        );

        String razorpayRefundId = razorpayRefund.get("id");

        // Update refund transaction with processing details
        JSONObject razorpayResponse = new JSONObject(razorpayRefund.toString());
        originalTransaction.setRefundProcessingFields(razorpayRefundId, razorpayResponse);

        // Update original transaction
        originalTransaction.setRefundStatus(PaymentTransaction.RefundStatus.PROCESSED);
        originalTransaction.setRefundProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(originalTransaction);

        // Save refund transaction
        PaymentTransaction savedRefund = paymentTransactionRepository.save(originalTransaction);

        // Create General Ledger entries
        ledgerService.recordWithdrawalProcessedSuccess(
                    originalTransaction.getRazorpayPaymentId(), originalTransaction.getTransactionId(),
                    request.getCustomerId(), originalTransaction.getOrderId(), request.getAmount(), getCurrentUserId());

        log.info("Refund processed with Razorpay: RefundId={}, RazorpayRefundId={}",
                savedRefund.getTransactionId(), razorpayRefundId);
        return savedRefund;
    }

    private void handleRefundCreated(JSONObject webhookPayload) {
        JSONObject refund = webhookPayload.getJSONObject("payload").getJSONObject("refund");
        String razorpayRefundId = refund.getString("id");
        String razorpayPaymentId = refund.getString("payment_id");

        log.info("Refund created in Razorpay: RefundId={}, PaymentId={}", razorpayRefundId, razorpayPaymentId);
    }

    private void handleRefundProcessed(JSONObject webhookPayload) {
        JSONObject refund = webhookPayload.getJSONObject("payload").getJSONObject("refund");
        String razorpayRefundId = refund.getString("id");

        // Update refund transaction if exists
        paymentTransactionRepository.findByRazorpayRefundId(razorpayRefundId).ifPresent(transaction -> {
            JSONObject razorpayResponse = new JSONObject(refund.toString());
            transaction.setRefundProcessingFields(razorpayRefundId, razorpayResponse);
            paymentTransactionRepository.save(transaction);

            log.info("Refund processed webhook received: RazorpayRefundId={}", razorpayRefundId);
        });
    }

    public RefundApprovalDTO handleRefundApproved(RefundApprovalDTO approvalRequest){
        ledgerService.recordRefundApproved(approvalRequest.getPaymentTransactionId(), approvalRequest.getTransactionId(),
                approvalRequest.getCustomerId(), approvalRequest.getOrderId(), approvalRequest.getAmount(), getCurrentUserId());
        log.info("Refund processed RefundApproved received: RazorpayRefundId={}", approvalRequest);
        return approvalRequest;
    };

    // ================================
    // API-8: GET PENDING REFUNDS
    // ================================
    public Page<PaymentTransaction> getPendingRefunds(int page, int size) {
        return paymentTransactionRepository.findByRefundApprovalStatus(
                PaymentTransaction.RefundApprovalStatus.PENDING,
                PageRequest.of(page, size, Sort.by("refundRequestedAt").descending())
        );
    }

    // ================================
    // ADDITIONAL HELPER METHODS
    // ================================
    public JSONObject getPaymentDetails(String razorpayPaymentId) throws RazorpayException {
        return razorpayClient.payments.fetch(razorpayPaymentId).toJson();
    }

    /**
     * Get user transactions with pagination
     */
    public Page<PaymentTransaction> getUserTransactions(String userId, int page, int size,
                                                        String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return paymentTransactionRepository.findByCustomerId(userId, pageRequest);
    }

    /**
     * Get recent user transactions (simple version)
     */
    public List<PaymentTransaction> getRecentUserTransactions(String userId, int limit) {
        // First get ALL transactions for user
        List<PaymentTransaction> allTransactions = paymentTransactionRepository
                .findAllByCustomerId(userId);

        // Sort by created date descending and limit
        return allTransactions.stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }


}