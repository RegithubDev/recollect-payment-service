package com.example.walletmicroservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundApprovalDTO {
    private String paymentTransactionId;
    private BigDecimal amount;
    private String approverId;
    private String transactionId;
    private String customerId;
    private String orderId;
}