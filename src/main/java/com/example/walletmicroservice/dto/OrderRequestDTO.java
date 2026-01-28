package com.example.walletmicroservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class OrderRequestDTO {
    private String customerId;
    private String orderId;
    private BigDecimal amount;
    private String currency = "INR";
    private String paymentMethod; // Optional: upi, card, netbanking, wallet
    private String description;
    private Map<String, Object> notes; // Additional metadata

    // No need for getNotes() method as Lombok @Data generates it
}