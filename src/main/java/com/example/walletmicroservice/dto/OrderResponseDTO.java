package com.example.walletmicroservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderResponseDTO {
    private String razorpayOrderId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String customerId;
    private String orderId;
    private String keyId; // Razorpay key ID for frontend
    private String status;
    private String createdAt; // Just store as string
}