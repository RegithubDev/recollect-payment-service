package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.dto.RazorpayQRCodeRequestDTO;
import com.example.walletmicroservice.dto.RazorpayQRCodeResponseDTO;
import com.example.walletmicroservice.service.RazorpayQRCodeService;
import com.razorpay.RazorpayException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/payments/qrcode")
@RequiredArgsConstructor
@Slf4j
public class RazorpayQRCodeController {

    private final RazorpayQRCodeService razorpayQRCodeService;

    /**
     * Create Dynamic QR Code for Order
     * POST /api/v1/payments/qrcode/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createQRCode(@RequestBody RazorpayQRCodeRequestDTO request) {
        try {
            RazorpayQRCodeResponseDTO response = razorpayQRCodeService.createQRCode(request);
            return ResponseEntity.ok(response);
        } catch (RazorpayException | IOException e) {
            log.error("Error creating QR code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error creating QR code: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Get QR Code Status
     * GET /api/v1/payments/qrcode/status/{transactionId}
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getQRCodeStatus(@PathVariable String transactionId) {
        try {
            RazorpayQRCodeResponseDTO response = razorpayQRCodeService.getQRCodeStatus(transactionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IOException e) {
            log.error("Error getting QR code status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error getting QR code status: " + e.getMessage()));
        }
    }

    /**
     * Check if QR Code payment is successful
     * GET /api/v1/payments/qrcode/check-payment/{transactionId}
     */
    @GetMapping("/check-payment/{transactionId}")
    public ResponseEntity<?> checkQRCodePayment(@PathVariable String transactionId) {
        try {
            boolean isSuccessful = razorpayQRCodeService.isQRCodePaymentSuccessful(transactionId);

            JSONObject response = new JSONObject();
            response.put("transactionId", transactionId);
            response.put("paymentSuccessful", isSuccessful);
            response.put("message", isSuccessful ? "Payment successful" : "Payment not yet received");

            return ResponseEntity.ok(response.toString());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IOException e) {
            log.error("Error checking QR code payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error checking payment: " + e.getMessage()));
        }
    }

    /**
     * Close QR Code
     * POST /api/v1/payments/qrcode/close/{transactionId}
     */
    @PostMapping("/close/{transactionId}")
    public ResponseEntity<?> closeQRCode(@PathVariable String transactionId) {
        try {
            razorpayQRCodeService.closeQRCode(transactionId);
            return ResponseEntity.ok(new SimpleResponse("QR code closed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IOException e) {
            log.error("Error closing QR code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error closing QR code: " + e.getMessage()));
        }
    }

    /**
     * Get QR Code Payments
     * GET /api/v1/payments/qrcode/payments/{transactionId}
     */
    @GetMapping("/payments/{transactionId}")
    public ResponseEntity<?> getQRCodePayments(@PathVariable String transactionId) {
        try {
            JSONObject payments = razorpayQRCodeService.getQRCodePayments(transactionId);
            return ResponseEntity.ok(payments.toString());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IOException e) {
            log.error("Error getting QR code payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error getting payments: " + e.getMessage()));
        }
    }

    // Response DTOs
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ErrorResponse {
        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class SimpleResponse {
        private String message;
    }
}