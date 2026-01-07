package com.example.walletmicroservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Capture payment request")
public class CaptureRequestDTO {

    @Schema(
            description = "Razorpay Payment ID",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "pay_29QQoUBi66xm2f"
    )
    @NotBlank(message = "Payment ID is required")
    private String razorpayPaymentId;

    @Schema(
            description = "Amount to capture",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1000.00"
    )
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}