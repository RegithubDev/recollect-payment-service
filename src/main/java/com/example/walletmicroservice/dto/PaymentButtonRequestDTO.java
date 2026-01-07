package com.example.walletmicroservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment link creation request")
public class PaymentButtonRequestDTO {

    @Schema(description = "Amount in rupees", requiredMode = Schema.RequiredMode.REQUIRED, example = "123.45")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum amount is ₹1.00")
    private BigDecimal amount;

    @Schema(description = "Currency", requiredMode = Schema.RequiredMode.REQUIRED, example = "INR")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Builder.Default
    private String currency = "INR";

    @Schema(description = "Payment description", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Payment for Invoice #123")
    @NotBlank(message = "Description is required")
    @Size(min = 3, max = 255, message = "Description must be 3-255 characters")
    private String description;

    @Schema(description = "Customer name", example = "John Doe")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String customerName;

    @Schema(description = "Customer email", example = "john@example.com")
    @Email(message = "Valid email required")
    private String customerEmail;

    @Schema(description = "Customer phone", example = "+919876543210")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Valid phone number required")
    private String customerPhone;

    @Schema(description = "Callback URL after payment",
            example = "https://yourdomain.com/api/payments/success")
    private String callbackUrl;

    @Schema(description = "Allow partial payments", example = "false")
    @Builder.Default
    private Boolean acceptPartial = false;

    @Schema(description = "Reference ID for tracking", example = "INV_123")
    private String referenceId;

    @Schema(description = "Additional notes/metadata")
    private Map<String, String> notes;
}