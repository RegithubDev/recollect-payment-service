
package com.example.walletmicroservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment verification response")
public class PaymentVerificationResponseDTO {

    @Schema(description = "Status", example = "success")
    private String status;

    @Schema(description = "Message", example = "Payment verified successfully")
    private String message;

    @Schema(description = "Transaction ID", example = "PAY1767690252307698")
    private String transactionId;

    @Schema(description = "Amount", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Payment status", example = "COMPLETED")
    private String paymentStatus;
}