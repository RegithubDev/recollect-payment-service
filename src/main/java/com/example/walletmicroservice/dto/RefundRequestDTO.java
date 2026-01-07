package com.example.walletmicroservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refund request")
public class RefundRequestDTO {

    @Schema(
            description = "Razorpay Payment ID",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "pay_29QQoUBi66xm2f"
    )
    @NotBlank(message = "Payment ID is required")
    private String razorpayPaymentId;

    @Schema(
            description = "Refund amount (optional - full refund if not specified)",
            example = "500.00"
    )
    private BigDecimal amount;

    @Schema(
            description = "Refund notes (optional)",
            example = "Customer requested refund"
    )
    private String notes;
}