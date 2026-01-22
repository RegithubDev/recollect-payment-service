package com.example.walletmicroservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequestDTO {
    private String paymentTransactionId; // Your internal transaction ID
    private BigDecimal amount;
    private String reason;
    private String notes; // Optional: JSON notes
}