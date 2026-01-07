package com.example.walletmicroservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment request details")
public class PaymentRequestDTO {

    @Schema(description = "Payment amount", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)", requiredMode = Schema.RequiredMode.REQUIRED, example = "INR")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Builder.Default
    private String currency = "INR";

    @Schema(description = "User ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @Schema(description = "Customer ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "cust_12345")
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @Schema(description = "Customer email", example = "customer@example.com")
    @Email(message = "Valid email is required")
    private String customerEmail;

    @Schema(description = "Customer phone number", example = "+919876543210")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Valid phone number required")
    private String customerPhone;

    @Schema(description = "Payment description", requiredMode = Schema.RequiredMode.REQUIRED, example = "Payment for Premium Subscription")
    @NotBlank(message = "Description is required")
    @Size(min = 3, max = 500, message = "Description must be 3-500 characters")
    private String description;

    @Schema(description = "Order ID (optional)", example = "ORD_67890")
    private String orderId;

    @Schema(description = "Product ID (optional)", example = "PROD_001")
    private String productId;

    @Schema(description = "Invoice ID (optional)", example = "INV_001")
    private String invoiceId;

    @Schema(description = "Additional notes (JSON string)", example = "{\"order_id\":\"order_67890\",\"product\":\"Premium Subscription\"}")
    private String notes;

    @Schema(description = "Additional metadata (key-value pairs)")
    private Map<String, Object> metadata;
}