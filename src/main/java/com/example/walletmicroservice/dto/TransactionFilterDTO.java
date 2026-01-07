// TransactionFilterDTO.java
package com.example.walletmicroservice.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class TransactionFilterDTO {
    private Long userId;
    private Long walletId;
    private String transactionType;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String referenceId;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isRefund;
    private Boolean isReversed;
    private Boolean isSettlement;
    private Boolean requiresApproval;
    private Boolean isApproved;
    private Boolean isProcessed;
    private Boolean isReconciled;
}