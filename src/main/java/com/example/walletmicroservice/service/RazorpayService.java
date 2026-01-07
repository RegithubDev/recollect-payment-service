package com.example.walletmicroservice.service;

import com.example.walletmicroservice.dto.PaymentRequestDTO;
import com.example.walletmicroservice.dto.PaymentResponseDTO;
import com.example.walletmicroservice.entity.Transaction;
import com.example.walletmicroservice.repository.TransactionRepository;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final TransactionRepository transactionRepository;

    @Value("${razorpay.callback.success:http://localhost:8080/api/payments/success}")
    private String successCallbackUrl;

    @Value("${razorpay.callback.failure:http://localhost:8080/api/payments/failure}")
    private String failureCallbackUrl;

    /**
     * Create a Razorpay order for payment
     */
    @Transactional
    public PaymentResponseDTO createPaymentOrder(PaymentRequestDTO request) throws RazorpayException {
        // Generate unique receipt
        String receiptId = "PAY" + System.currentTimeMillis() + new Random().nextInt(1000);

        // If orderId not provided, generate one
        String orderId = request.getOrderId() != null ? request.getOrderId() : "ORD" + System.currentTimeMillis();

        try {
            // Create order in Razorpay
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).longValue()); // Convert to paise
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            orderRequest.put("receipt", receiptId);
            orderRequest.put("payment_capture", 1); // Auto-capture payment

            // Add notes for tracking
            JSONObject notes = new JSONObject();
            notes.put("userId", request.getUserId() != null ? request.getUserId().toString() : "anonymous");
            notes.put("orderId", orderId);
            notes.put("productId", request.getProductId());
            notes.put("invoiceId", request.getInvoiceId());
            notes.put("description", request.getDescription());

            // Add custom metadata if provided
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(notes::put);
            }

            orderRequest.put("notes", notes);

            Order order = razorpayClient.orders.create(orderRequest);
            log.info("Razorpay order created: {}", order);

            // Create transaction record
            // In RazorpayService.createPaymentOrder()
            Transaction transaction = Transaction.builder()
                    .transactionId(receiptId)
                    .userId(request.getUserId() != null ? request.getUserId() : 1L)
                    .walletId(0L)
                    .amount(request.getAmount())
                    .type(Transaction.TransactionType.PAYMENT)
                    .status(Transaction.TransactionStatus.PENDING)
                    .paymentMethod(Transaction.PaymentMethod.RAZORPAY)
                    .sourceType(Transaction.SourceType.RAZORPAY)
                    .sourceReference(order.get("id"))
                    .description(request.getDescription() != null ? request.getDescription() :
                            "Payment for " + request.getCustomerEmail())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .feeAmount(BigDecimal.ZERO)  // EXPLICITLY SET
                    .taxAmount(BigDecimal.ZERO)  // EXPLICITLY SET
                    .netAmount(request.getAmount())
                    .razorpayOrderId(order.get("id"))
                    .referenceId(orderId)
                    .transactionMode("ONLINE")
                    .notes(request.getNotes())
                    .metadata(notes != null ? notes.toString() : "{}")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    // EXPLICITLY SET ALL BOOLEAN FIELDS
                    .isRefund(false)
                    .isDisputed(false)
                    .isReversed(false)
                    .isSettlement(false)
                    .requiresApproval(false)
                    .isApproved(true)
                    .isProcessed(false)
                    .isReconciled(false)
                    .build();

            transactionRepository.save(transaction);

            // Build response
            return PaymentResponseDTO.builder()
                    .transactionId(transaction.getTransactionId())
                    .razorpayOrderId(order.get("id"))
                    .amount(request.getAmount())
                    .currency(order.get("currency"))
                    .status(order.get("status"))
                    .description(request.getDescription())
                    .createdAt(transaction.getCreatedAt())
                    .callbackUrl(successCallbackUrl)
                    .notes(request.getNotes())
                    .customerEmail(request.getCustomerEmail())
                    .customerPhone(request.getCustomerPhone())
                    .build();

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Verify payment and update transaction
     */
    @Transactional
    public Transaction verifyPayment(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        try {
            // Verify payment signature
            boolean isValidSignature = verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

            if (!isValidSignature) {
                throw new RuntimeException("Invalid payment signature");
            }

            // Fetch payment details from Razorpay
            Payment payment = razorpayClient.payments.fetch(razorpayPaymentId);

            // Find transaction by order ID
            Transaction transaction = transactionRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            if (payment.get("status").equals("captured")) {
                // Update transaction as completed
                transaction.setRazorpayPaymentId(razorpayPaymentId);
                transaction.setReferenceId(payment.get("id"));
                transaction.markCompleted(); // No wallet balance tracking
                transaction.setProcessed(true);
                transaction.setProcessedAt(LocalDateTime.now());

                // Capture payment method details
                JSONObject paymentDetails = new JSONObject(payment.toString());
                if (paymentDetails.has("method")) {
                    transaction.setPaymentMethod(
                            Transaction.PaymentMethod.valueOf(paymentDetails.getString("method").toUpperCase())
                    );
                }

                log.info("Payment verified successfully for order: {}", razorpayOrderId);
            } else {
                transaction.markFailed("Payment not captured");
                log.warn("Payment not captured for order: {}", razorpayOrderId);
            }

            return transactionRepository.save(transaction);

        } catch (RazorpayException e) {
            log.error("Error verifying payment: {}", e.getMessage());
            throw new RuntimeException("Payment verification failed", e);
        }
    }

    /**
     * Verify Razorpay signature
     */
    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            // For testing, skip signature verification
            if (isTestMode()) {
                log.warn("TEST MODE: Skipping signature verification");
                return true;
            }

            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String secret = "ZGmyOTRuMxQnOwMKZU623UZt"; // Get from properties

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] signature = mac.doFinal(payload.getBytes());

            String generatedSignature = bytesToHex(signature);
            return generatedSignature.equals(razorpaySignature);

        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTestMode() {
        // Check if we're in test mode
        return true; // Set to false for production
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Handle Razorpay webhook events
     */
    @Transactional
    public void handleWebhook(Map<String, Object> webhookData) {
        try {
            String event = (String) webhookData.get("event");
            Map<String, Object> payload = (Map<String, Object>) webhookData.get("payload");

            if (payload == null) {
                log.error("Webhook payload is null");
                return;
            }

            Map<String, Object> paymentEntity = (Map<String, Object>) payload.get("payment");
            if (paymentEntity == null) {
                paymentEntity = (Map<String, Object>) payload.get("payment_link");
            }

            if (paymentEntity == null) {
                log.error("Payment entity not found in webhook");
                return;
            }

            String paymentId = (String) paymentEntity.get("id");
            String orderId = (String) paymentEntity.get("order_id");

            switch (event) {
                case "payment.captured":
                    handlePaymentCaptured(paymentId, orderId, paymentEntity);
                    break;
                case "payment.failed":
                    handlePaymentFailed(paymentId, orderId, paymentEntity);
                    break;
                case "payment.refunded":
                    handlePaymentRefunded(paymentId, orderId, paymentEntity);
                    break;
                case "refund.created":
                    handleRefundCreated(paymentId, orderId, paymentEntity);
                    break;
                case "order.paid":
                    handleOrderPaid(orderId, paymentEntity);
                    break;
                default:
                    log.info("Unhandled webhook event: {}", event);
            }

        } catch (Exception e) {
            log.error("Error handling webhook: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentCaptured(String paymentId, String orderId, Map<String, Object> paymentEntity) {
        Transaction transaction = transactionRepository.findByRazorpayOrderId(orderId)
                .orElse(null);

        // FIX: Use getter isProcessed() instead of setter setProcessed()
        if (transaction != null && !transaction.getIsProcessed()) {
            BigDecimal amount = new BigDecimal(paymentEntity.get("amount").toString())
                    .divide(new BigDecimal("100"));

            transaction.setRazorpayPaymentId(paymentId);
            transaction.markCompleted(); // Just for tracking
            transaction.setProcessed(true);
            // Update payment method from webhook
            String method = (String) paymentEntity.get("method");
            if (method != null) {
                try {
                    transaction.setPaymentMethod(Transaction.PaymentMethod.valueOf(method.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown payment method: {}", method);
                }
            }

            transactionRepository.save(transaction);

            log.info("Webhook: Payment captured for order: {}", orderId);
        }
    }

    private void handlePaymentFailed(String paymentId, String orderId, Map<String, Object> paymentEntity) {
        Transaction transaction = transactionRepository.findByRazorpayOrderId(orderId)
                .orElse(null);

        if (transaction != null) {
            String errorDescription = (String) paymentEntity.get("error_description");
            String errorCode = (String) paymentEntity.get("error_code");

            String failureReason = errorDescription != null ? errorDescription : "Payment failed";
            if (errorCode != null) {
                failureReason = "[" + errorCode + "] " + failureReason;
            }

            transaction.markFailed(failureReason);
            transactionRepository.save(transaction);

            log.warn("Webhook: Payment failed for order: {}, reason: {}", orderId, failureReason);
        }
    }

    private void handlePaymentRefunded(String paymentId, String orderId, Map<String, Object> paymentEntity) {
        Transaction transaction = transactionRepository.findByRazorpayPaymentId(paymentId)
                .orElse(null);

        if (transaction != null) {
            // Create refund transaction
            BigDecimal refundAmount = new BigDecimal(paymentEntity.get("amount_refunded").toString())
                    .divide(new BigDecimal("100"));

            Transaction refundTransaction = Transaction.builder()
                    .userId(transaction.getUserId())
                    .amount(refundAmount)
                    .type(Transaction.TransactionType.REFUND)
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .paymentMethod(Transaction.PaymentMethod.RAZORPAY)
                    .sourceType(Transaction.SourceType.REFUND)
                    .sourceReference(paymentId)
                    .description("Refund for payment: " + paymentId)
                    .currency(transaction.getCurrency())
                    .netAmount(refundAmount)
                    .razorpayPaymentId(paymentId)
                    .referenceId((String) paymentEntity.get("refund_id"))
                    .parentTransactionId(transaction.getTransactionId())
                    .isRefund(true)
                    .isProcessed(true)
                    .processedAt(LocalDateTime.now())
                    .metadata(paymentEntity.toString())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(refundTransaction);
            transaction.markRefunded();
            transactionRepository.save(transaction);

            log.info("Webhook: Payment refunded for payment: {}", paymentId);
        }
    }

    private void handleRefundCreated(String paymentId, String orderId, Map<String, Object> refundEntity) {
        log.info("Refund created: {}", refundEntity);
        // Handle refund creation separately if needed
    }

    private void handleOrderPaid(String orderId, Map<String, Object> orderEntity) {
        log.info("Order paid: {}", orderEntity);
        // Handle order paid event
    }

    /**
     * Get payment status
     */
    public Map<String, Object> getPaymentStatus(String transactionId) throws RazorpayException {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Map<String, Object> status = new HashMap<>();
        status.put("transactionId", transaction.getTransactionId());
        status.put("razorpayOrderId", transaction.getRazorpayOrderId());
        status.put("razorpayPaymentId", transaction.getRazorpayPaymentId());
        status.put("amount", transaction.getAmount());
        status.put("status", transaction.getStatus().toString());
        status.put("currency", transaction.getCurrency());
        status.put("createdAt", transaction.getCreatedAt());
        status.put("processedAt", transaction.getProcessedAt());
        status.put("description", transaction.getDescription());
        status.put("paymentMethod", transaction.getPaymentMethod());
        status.put("referenceId", transaction.getReferenceId());

        if (transaction.getRazorpayPaymentId() != null) {
            try {
                Payment payment = razorpayClient.payments.fetch(transaction.getRazorpayPaymentId());
                status.put("razorpayPaymentStatus", payment.get("status"));
                status.put("razorpayPaymentMethod", payment.get("method"));
                status.put("razorpayBank", payment.get("bank"));
                status.put("razorpayCardId", payment.get("card_id"));
                status.put("razorpayVpa", payment.get("vpa"));
                status.put("razorpayWallet", payment.get("wallet"));
                status.put("razorpayEmail", payment.get("email"));
                status.put("razorpayContact", payment.get("contact"));
            } catch (RazorpayException e) {
                log.warn("Could not fetch payment details from Razorpay: {}", e.getMessage());
            }
        }

        return status;
    }

    /**
     * Get all transactions for a user
     */
    public List<Transaction> getUserTransactions(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Get transaction by razorpay order ID
     */
    public Optional<Transaction> getTransactionByOrderId(String razorpayOrderId) {
        return transactionRepository.findByRazorpayOrderId(razorpayOrderId);
    }

    /**
     * Create a refund for a payment
     */
    @Transactional
    public Map<String, Object> createRefund(String razorpayPaymentId, BigDecimal amount, String notes) throws RazorpayException {
        try {
            // Create refund in Razorpay
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("payment_id", razorpayPaymentId);

            if (amount != null) {
                refundRequest.put("amount", amount.multiply(new BigDecimal("100")).longValue());
            }

            if (notes != null) {
                refundRequest.put("notes", new JSONObject().put("reason", notes));
            }

            // Call Razorpay API
            com.razorpay.Refund refund = razorpayClient.payments.refund(razorpayPaymentId, refundRequest);

            // Find original transaction
            Transaction originalTransaction = transactionRepository.findByRazorpayPaymentId(razorpayPaymentId)
                    .orElseThrow(() -> new RuntimeException("Original transaction not found"));

            // Create refund transaction record
            BigDecimal refundAmount = new BigDecimal(refund.get("amount").toString())
                    .divide(new BigDecimal("100"));

            Transaction refundTransaction = Transaction.builder()
                    .userId(originalTransaction.getUserId())
                    .amount(refundAmount)
                    .type(Transaction.TransactionType.REFUND)
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .paymentMethod(Transaction.PaymentMethod.RAZORPAY)
                    .sourceType(Transaction.SourceType.REFUND)
                    .sourceReference(refund.get("id"))
                    .description("Manual refund: " + (notes != null ? notes : "Refund initiated"))
                    .currency(originalTransaction.getCurrency())
                    .netAmount(refundAmount)
                    .razorpayPaymentId(razorpayPaymentId)
                    .referenceId(refund.get("id"))
                    .parentTransactionId(originalTransaction.getTransactionId())
                    .isRefund(true)
                    .isProcessed(true)
                    .processedAt(LocalDateTime.now())
                    .notes(notes)
                    .metadata(refund.toString())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(refundTransaction);

            // Mark original transaction as refunded
            originalTransaction.markRefunded();
            transactionRepository.save(originalTransaction);

            Map<String, Object> response = new HashMap<>();
            response.put("refundId", refund.get("id"));
            response.put("amount", refundAmount);
            response.put("status", refund.get("status"));
            response.put("transactionId", refundTransaction.getTransactionId());
            response.put("message", "Refund initiated successfully");

            return response;

        } catch (RazorpayException e) {
            log.error("Error creating refund: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Capture authorized payment (for manual capture)
     */
    @Transactional
    public Map<String, Object> capturePayment(String razorpayPaymentId, BigDecimal amount) throws RazorpayException {
        try {
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", amount.multiply(new BigDecimal("100")).longValue());

            com.razorpay.Payment payment = razorpayClient.payments.capture(razorpayPaymentId, captureRequest);

            // Update transaction status
            Transaction transaction = transactionRepository.findByRazorpayPaymentId(razorpayPaymentId)
                    .orElse(null);

            if (transaction != null) {
                transaction.markCompleted();
                transaction.setProcessed(true);
                transaction.setProcessedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.get("id"));
            response.put("amount", new BigDecimal(payment.get("amount").toString()).divide(new BigDecimal("100")));
            response.put("status", payment.get("status"));
            response.put("captured", payment.get("captured"));
            response.put("message", "Payment captured successfully");

            return response;

        } catch (RazorpayException e) {
            log.error("Error capturing payment: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get payment details from Razorpay
     */
    public Map<String, Object> getPaymentDetails(String razorpayPaymentId) throws RazorpayException {
        try {
            Payment payment = razorpayClient.payments.fetch(razorpayPaymentId);

            Map<String, Object> details = new HashMap<>();
            details.put("id", payment.get("id"));
            details.put("amount", new BigDecimal(payment.get("amount").toString()).divide(new BigDecimal("100")));
            details.put("currency", payment.get("currency"));
            details.put("status", payment.get("status"));
            details.put("method", payment.get("method"));
            details.put("order_id", payment.get("order_id"));
            details.put("created_at", payment.get("created_at"));

            if (payment.has("bank")) details.put("bank", payment.get("bank"));
            if (payment.has("card_id")) details.put("card_id", payment.get("card_id"));
            if (payment.has("vpa")) details.put("vpa", payment.get("vpa"));
            if (payment.has("wallet")) details.put("wallet", payment.get("wallet"));
            if (payment.has("email")) details.put("email", payment.get("email"));
            if (payment.has("contact")) details.put("contact", payment.get("contact"));

            return details;

        } catch (RazorpayException e) {
            log.error("Error fetching payment details: {}", e.getMessage());
            throw e;
        }
    }
}