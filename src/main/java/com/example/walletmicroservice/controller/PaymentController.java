package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.dto.*;
import com.example.walletmicroservice.entity.Transaction;
import com.example.walletmicroservice.service.RazorpayService;
import com.razorpay.RazorpayException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment API", description = "Payment processing endpoints with Razorpay integration")
public class PaymentController {

    private final RazorpayService razorpayService;

    @Operation(
            summary = "Create payment order",
            description = "Creates a new payment order in Razorpay and returns payment details",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment order created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                      "transactionId": "PAY1767690252307698",
                      "razorpayOrderId": "order_S0XLPXxKd5eHFi",
                      "razorpayPaymentId": null,
                      "amount": 1000.00,
                      "currency": "INR",
                      "status": "created",
                      "paymentMethod": null,
                      "description": "Payment for Premium Subscription",
                      "createdAt": "2026-01-06T14:34:12.800036548",
                      "callbackUrl": "http://localhost:8080/api/payments/success",
                      "notes": "{\\"order_id\\":\\"order_67890\\",\\"product\\":\\"Premium Subscription\\"}",
                      "customerEmail": "customer@example.com",
                      "customerPhone": "+919876543210"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Validation failed",
                      "message": "Amount must be at least 0.01"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - API key missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid API key"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Internal Server Error",
                      "message": "Payment creation failed: Razorpay service unavailable"
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment request details",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "amount": 1000.00,
                          "currency": "INR",
                          "userId": 123,
                          "customerId": "cust_12345",
                          "customerEmail": "customer@example.com",
                          "customerPhone": "+919876543210",
                          "description": "Payment for Premium Subscription",
                          "notes": "{\\"order_id\\":\\"order_67890\\",\\"product\\":\\"Premium Subscription\\"}",
                          "orderId": "ORD_67890",
                          "productId": "PROD_001"
                        }
                        """
                            )
                    )
            )
            @Valid @RequestBody PaymentRequestDTO request) {
        try {
            PaymentResponseDTO response = razorpayService.createPaymentOrder(request);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error creating payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment creation failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Verify payment",
            description = "Verifies a payment using Razorpay payment ID, order ID and signature",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment verified successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentVerificationResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                      "status": "success",
                      "message": "Payment verified successfully",
                      "transactionId": "PAY1767690252307698",
                      "amount": 1000.00,
                      "currency": "INR",
                      "paymentStatus": "COMPLETED"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payment details or signature",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Payment verification failed",
                      "message": "Invalid payment signature"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid API key"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Transaction not found",
                      "message": "No transaction found with order ID: order_S0XLPXxKd5eHFi"
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment verification request",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentVerificationDTO.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "razorpayPaymentId": "pay_29QQoUBi66xm2f",
                          "razorpayOrderId": "order_S0XLPXxKd5eHFi",
                          "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
                        }
                        """
                            )
                    )
            )
            @Valid @RequestBody PaymentVerificationDTO verificationRequest) {
        try {
            Transaction transaction = razorpayService.verifyPayment(
                    verificationRequest.getRazorpayPaymentId(),
                    verificationRequest.getRazorpayOrderId(),
                    verificationRequest.getRazorpaySignature());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Payment verified successfully",
                    "transactionId", transaction.getTransactionId(),
                    "amount", transaction.getAmount(),
                    "currency", transaction.getCurrency(),
                    "paymentStatus", transaction.getStatus()
            ));
        } catch (Exception e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment verification failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get payment status",
            description = "Retrieves the status of a payment transaction",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment status retrieved",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "transactionId": "PAY1767690252307698",
                      "razorpayOrderId": "order_S0XLPXxKd5eHFi",
                      "razorpayPaymentId": "pay_29QQoUBi66xm2f",
                      "amount": 1000.00,
                      "status": "COMPLETED",
                      "currency": "INR",
                      "createdAt": "2026-01-06T14:34:12.800036548",
                      "processedAt": "2026-01-06T14:34:15.123456789",
                      "description": "Payment for Premium Subscription",
                      "paymentMethod": "RAZORPAY",
                      "referenceId": "ORD_67890"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid API key"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Transaction not found",
                      "message": "No transaction found with ID: PAY1767690252307698"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getPaymentStatus(
            @Parameter(
                    description = "Transaction ID",
                    required = true,
                    example = "PAY1767690252307698",
                    schema = @Schema(type = "string", minLength = 10, maxLength = 50)
            )
            @PathVariable String transactionId) {
        try {
            Map<String, Object> status = razorpayService.getPaymentStatus(transactionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting payment status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Transaction not found", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get payment details",
            description = "Retrieves detailed information about a payment from Razorpay",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment details retrieved",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "id": "pay_29QQoUBi66xm2f",
                      "amount": 1000.00,
                      "currency": "INR",
                      "status": "captured",
                      "method": "card",
                      "order_id": "order_S0XLPXxKd5eHFi",
                      "created_at": 1672531199,
                      "bank": "HDFC",
                      "card_id": "card_E0y8vaKvWqA8Vp",
                      "email": "customer@example.com",
                      "contact": "+919876543210"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid API key"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Payment not found",
                      "message": "No payment found with ID: pay_29QQoUBi66xm2f"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/details/{paymentId}")
    public ResponseEntity<?> getPaymentDetails(
            @Parameter(
                    description = "Razorpay Payment ID",
                    required = true,
                    example = "pay_29QQoUBi66xm2f",
                    schema = @Schema(type = "string", pattern = "^pay_[A-Za-z0-9]+$")
            )
            @PathVariable String paymentId) {
        try {
            Map<String, Object> details = razorpayService.getPaymentDetails(paymentId);
            return ResponseEntity.ok(details);
        } catch (RazorpayException e) {
            log.error("Error getting payment details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Payment not found", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Create refund",
            description = "Creates a refund for a payment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refund created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "refundId": "rfnd_F4bqJqjNcDlL3V",
                      "amount": 500.00,
                      "status": "processed",
                      "transactionId": "REF1234567890",
                      "message": "Refund initiated successfully"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid refund request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Refund failed",
                      "message": "Payment cannot be refunded"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid API key"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Payment not found",
                      "message": "No payment found with ID: pay_29QQoUBi66xm2f"
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping("/refund")
    public ResponseEntity<?> createRefund(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refund request",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefundRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "razorpayPaymentId": "pay_29QQoUBi66xm2f",
                          "amount": 500.00,
                          "notes": "Customer requested partial refund"
                        }
                        """
                            )
                    )
            )
            @Valid @RequestBody RefundRequestDTO refundRequest) {
        try {
            Map<String, Object> response = razorpayService.createRefund(
                    refundRequest.getRazorpayPaymentId(),
                    refundRequest.getAmount(),
                    refundRequest.getNotes());
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error creating refund: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refund failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Capture authorized payment",
            description = "Captures a pre-authorized payment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment captured successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "paymentId": "pay_29QQoUBi66xm2f",
                      "amount": 1000.00,
                      "status": "captured",
                      "captured": true,
                      "message": "Payment captured successfully"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid capture request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Capture failed",
                      "message": "Payment already captured"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid API key"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Payment not found",
                      "message": "No payment found with ID: pay_29QQoUBi66xm2f"
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping("/capture")
    public ResponseEntity<?> capturePayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Capture payment request",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CaptureRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "razorpayPaymentId": "pay_29QQoUBi66xm2f",
                          "amount": 1000.00
                        }
                        """
                            )
                    )
            )
            @Valid @RequestBody CaptureRequestDTO captureRequest) {
        try {
            Map<String, Object> response = razorpayService.capturePayment(
                    captureRequest.getRazorpayPaymentId(),
                    captureRequest.getAmount());
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error capturing payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Capture failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get user transactions",
            description = "Retrieves all transactions for a specific user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User transactions retrieved",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    [
                      {
                        "id": 1,
                        "transactionId": "PAY1767690252307698",
                        "userId": 123,
                        "amount": 1000.00,
                        "type": "PAYMENT",
                        "status": "COMPLETED",
                        "paymentMethod": "RAZORPAY",
                        "description": "Payment for Premium Subscription",
                        "currency": "INR",
                        "createdAt": "2026-01-06T14:34:12.800036548",
                        "processedAt": "2026-01-06T14:34:15.123456789"
                      },
                      {
                        "id": 2,
                        "transactionId": "PAY1767690252307699",
                        "userId": 123,
                        "amount": 500.00,
                        "type": "REFUND",
                        "status": "COMPLETED",
                        "paymentMethod": "RAZORPAY",
                        "description": "Refund for order #67890",
                        "currency": "INR",
                        "createdAt": "2026-01-06T15:30:00.000000000"
                      }
                    ]
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid API key"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Internal Server Error",
                      "message": "Failed to fetch transactions"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTransactions(
            @Parameter(
                    description = "User ID",
                    required = true,
                    example = "123",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            @PathVariable Long userId) {
        try {
            List<Transaction> transactions = razorpayService.getUserTransactions(userId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error fetching user transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transactions", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Handle webhook",
            description = "Endpoint for Razorpay webhook notifications",
            security = @SecurityRequirement(name = "X-Razorpay-Signature")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Webhook processed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "status": "success"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid webhook payload or signature",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Webhook processing failed",
                      "message": "Invalid signature"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid signature",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid X-Razorpay-Signature header"
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @Parameter(
                    description = "Webhook payload from Razorpay",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                        {
                          "event": "payment.captured",
                          "payload": {
                            "payment": {
                              "entity": {
                                "id": "pay_29QQoUBi66xm2f",
                                "amount": 100000,
                                "currency": "INR",
                                "status": "captured",
                                "order_id": "order_S0XLPXxKd5eHFi",
                                "method": "card"
                              }
                            }
                          }
                        }
                        """
                            )
                    )
            )
            @RequestBody Map<String, Object> webhookData,
            @Parameter(
                    description = "Razorpay webhook signature",
                    required = true,
                    example = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0",
                    schema = @Schema(type = "string")
            )
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            razorpayService.handleWebhook(webhookData);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Webhook processing failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Health check",
            description = "Check if payment service is healthy"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                    {
                      "status": "UP",
                      "service": "payment-service",
                      "timestamp": "2026-01-06T14:34:12.800036548"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-service",
                "timestamp", java.time.LocalDateTime.now()
        ));
    }
}