package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.dto.OrderRequestDTO;
import com.example.walletmicroservice.dto.OrderResponseDTO;
import com.example.walletmicroservice.dto.RefundRequestDTO;
import com.example.walletmicroservice.entity.PaymentTransaction;
import com.example.walletmicroservice.service.RazorpayService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SignatureException;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class RazorpayController {

    private final RazorpayService razorpayService;

    /**
     * API-1: Create Order
     * POST /api/v1/payments/create-order
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDTO request) {
        try {
            OrderResponseDTO response = razorpayService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating order: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * API-3: Razorpay Webhook Endpoint
     * POST /api/v1/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String razorpaySignature) {

        try {
            // Verify webhook signature
            String secret = System.getenv("RAZORPAY_WEBHOOK_SECRET");
            String expectedSignature = calculateRFC2104HMAC(payload, secret);

            if (!expectedSignature.equals(razorpaySignature)) {
                log.error("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }

            JSONObject webhookPayload = new JSONObject(payload);
            razorpayService.handleWebhook(webhookPayload);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * API-4: Create Refund
     * POST /api/v1/payments/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<?> createRefund(@RequestBody RefundRequestDTO request) {
        try {
            PaymentTransaction refund = razorpayService.createRefund(request);
            return ResponseEntity.ok(refund);
        } catch (RazorpayException e) {
            log.error("Error creating refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating refund: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Get payment status
     * GET /api/v1/payments/status/{transactionId}
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String transactionId) {
        try {
            PaymentTransaction transaction = razorpayService.getPaymentTransaction(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Transaction not found");
        }
    }

    /**
     * Verify payment (for QR code/UPI)
     * POST /api/v1/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestParam String razorpayPaymentId,
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpaySignature) {

        try {
            // 1️⃣ Verify signature first
            boolean isValid = razorpayService.verifySignature(
                    razorpayPaymentId, razorpayOrderId, razorpaySignature);

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Payment verification failed");
            }

            // 2️⃣ Fetch payment status via SDK
            String status = razorpayService.getPaymentStatus(razorpayPaymentId, razorpayOrderId);

            return ResponseEntity.ok().body(status);

        } catch (Exception e) {
            log.error("Error verifying payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error verifying payment");
        }
    }


    private String calculateRFC2104HMAC(String data, String secret) throws java.security.SignatureException {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC: " + e.getMessage());
        }
    }
}