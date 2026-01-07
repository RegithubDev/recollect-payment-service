package com.example.walletmicroservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment response details")
public class PaymentResponseDTO {

    @Schema(description = "Transaction ID", example = "TXN1234567890")
    private String transactionId;

    @Schema(description = "Razorpay Order ID", example = "order_S0XLPXxKd5eHFi")
    private String razorpayOrderId;

    @Schema(description = "Razorpay Payment ID", example = "pay_29QQoUBi66xm2f")
    private String razorpayPaymentId;

    @Schema(description = "Payment amount", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Payment status", example = "created")
    private String status;

    @Schema(description = "Payment method", example = "RAZORPAY")
    private String paymentMethod;

    @Schema(description = "Payment description", example = "Payment for Premium Subscription")
    private String description;

    @Schema(description = "Transaction creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Callback URL for payment completion", example = "http://localhost:8080/api/payments/success")
    private String callbackUrl;

    @Schema(description = "Additional notes", example = "{\"order_id\":\"order_67890\"}")
    private String notes;

    @Schema(description = "Customer email", example = "customer@example.com")
    private String customerEmail;

    @Schema(description = "Customer phone", example = "+919876543210")
    private String customerPhone;
}