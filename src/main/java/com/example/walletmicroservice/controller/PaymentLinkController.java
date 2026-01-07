package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.dto.PaymentButtonRequestDTO;
import com.example.walletmicroservice.dto.PaymentButtonResponseDTO;
import com.example.walletmicroservice.service.PaymentLinkService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/payment-links") // Changed from /api/payment-buttons
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Links API", description = "Create and manage Razorpay payment links for custom amounts")
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;

    @Operation(
            summary = "Create payment link",
            description = "Creates a Razorpay payment link for custom amounts with 30-minute expiry",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment link created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentButtonResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "paymentLinkId": "plink_1234567890",
                                      "shortUrl": "https://rzp.io/i/abc123",
                                      "url": "https://rzp.io/l/abc123",
                                      "amount": "123.45",
                                      "currency": "INR",
                                      "description": "Payment for Invoice #123",
                                      "status": "created",
                                      "createdAt": "2024-01-07 15:30:00",
                                      "expiresAt": "2024-01-07 16:00:00",
                                      "embedHtml": "<html>...payment link HTML...</html>",
                                      "metadata": {
                                        "type": "PAYMENT_LINK",
                                        "expires_in_minutes": 30,
                                        "is_reusable": false
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create")
    public ResponseEntity<?> createPaymentLink(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment link creation request",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentButtonRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "amount": 123.45,
                                      "currency": "INR",
                                      "description": "Payment for Invoice #123",
                                      "customerName": "John Doe",
                                      "customerEmail": "john@example.com",
                                      "customerPhone": "+919876543210",
                                      "callbackUrl": "https://yourdomain.com/api/payments/success",
                                      "acceptPartial": false,
                                      "referenceId": "INV_123",
                                      "notes": {
                                        "invoice_number": "INV-2024-001",
                                        "product": "Premium Subscription"
                                      }
                                    }
                                    """
                            )
                    )
            )
            @Valid @RequestBody PaymentButtonRequestDTO request) {
        try {
            log.info("Received payment link creation request for amount: ₹{}", request.getAmount());

            // Validate at least one customer contact method
            if (request.getCustomerEmail() == null && request.getCustomerPhone() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Validation failed",
                                "message", "Either customer email or phone is required"
                        ));
            }

            PaymentButtonResponseDTO response = paymentLinkService.createPaymentLink(request);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error creating payment link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment link creation failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get payment link details",
            description = "Retrieves details of a payment link",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{paymentLinkId}")
    public ResponseEntity<?> getPaymentLink(
            @Parameter(description = "Payment Link ID", required = true, example = "plink_1234567890")
            @PathVariable String paymentLinkId) {
        try {
            PaymentButtonResponseDTO response = paymentLinkService.getPaymentLink(paymentLinkId);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error fetching payment link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Payment link not found", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get payment link statistics",
            description = "Get payment link status, expiry time, etc.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{paymentLinkId}/stats")
    public ResponseEntity<?> getPaymentLinkStats(
            @Parameter(description = "Payment Link ID", required = true)
            @PathVariable String paymentLinkId) {
        try {
            Map<String, Object> response = paymentLinkService.getPaymentLinkStats(paymentLinkId);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error fetching payment link stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Failed to fetch payment link stats", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Cancel payment link",
            description = "Cancels a payment link",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{paymentLinkId}/cancel")
    public ResponseEntity<?> cancelPaymentLink(
            @Parameter(description = "Payment Link ID", required = true)
            @PathVariable String paymentLinkId) {
        try {
            Map<String, Object> response = paymentLinkService.cancelPaymentLink(paymentLinkId);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error cancelling payment link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment link cancellation failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Send payment link",
            description = "Sends payment link via email/SMS",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{paymentLinkId}/send")
    public ResponseEntity<?> sendPaymentLink(
            @Parameter(description = "Payment Link ID", required = true)
            @PathVariable String paymentLinkId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        try {
            Map<String, Object> response = paymentLinkService.sendPaymentLink(paymentLinkId, email, phone);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Error sending payment link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to send payment link", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Direct payment page",
            description = "Get a complete HTML payment page for the payment link",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{paymentLinkId}/page")
    public ResponseEntity<String> getPaymentPage(
            @Parameter(description = "Payment Link ID", required = true)
            @PathVariable String paymentLinkId) {
        try {
            PaymentButtonResponseDTO paymentLink = paymentLinkService.getPaymentLink(paymentLinkId);

            // Return the HTML directly
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(paymentLink.getEmbedHtml());

        } catch (RazorpayException e) {
            String errorHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Error</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif; 
                            background: #f8f9fa; 
                            display: flex; 
                            justify-content: center; 
                            align-items: center; 
                            height: 100vh; 
                            margin: 0; 
                        }
                        .error-container { 
                            background: white; 
                            padding: 40px; 
                            border-radius: 10px; 
                            box-shadow: 0 10px 30px rgba(0,0,0,0.1); 
                            text-align: center; 
                            max-width: 400px; 
                        }
                        h1 { color: #e74c3c; margin-bottom: 20px; }
                        p { color: #666; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="error-container">
                        <h1>Payment Link Error</h1>
                        <p>%s</p>
                        <a href="/" style="color: #3498db; text-decoration: none;">Go Back</a>
                    </div>
                </body>
                </html>
                """.formatted(e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }
}