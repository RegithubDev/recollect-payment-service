package com.example.walletmicroservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayoutWalletDTO {
    private String payoutId;
    private String razorpayPayoutId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String mode;
    private String purpose;
    private String referenceId;
    private LocalDateTime createdAt;
    private String utrNumber;
    private BigDecimal fees;
    private BigDecimal tax;
}