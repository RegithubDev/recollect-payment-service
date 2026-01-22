package com.example.walletmicroservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequestDTO {
    private String customerId;
    private String orderId;
    private BigDecimal amount;
    private String currency = "INR";
    private String paymentMethod; // Optional: upi, card, netbanking, wallet
    private String description;
}