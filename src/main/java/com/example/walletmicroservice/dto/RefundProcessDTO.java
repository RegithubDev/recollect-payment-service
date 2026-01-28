package com.example.walletmicroservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundProcessDTO {
    private String refundRequestId;
    private String razorpayRefundId;
    private String transactionId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime processedAt;
}