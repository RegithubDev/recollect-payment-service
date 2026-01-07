package com.example.walletmicroservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment verification request")
public class PaymentVerificationDTO {

    @Schema(
            description = "Razorpay Payment ID",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "pay_29QQoUBi66xm2f"
    )
    @NotBlank(message = "Razorpay Payment ID is required")
    private String razorpayPaymentId;

    @Schema(
            description = "Razorpay Order ID",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "order_S0XLPXxKd5eHFi"
    )
    @NotBlank(message = "Razorpay Order ID is required")
    private String razorpayOrderId;

    @Schema(
            description = "Razorpay Signature",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
    )
    @NotBlank(message = "Razorpay Signature is required")
    private String razorpaySignature;
}