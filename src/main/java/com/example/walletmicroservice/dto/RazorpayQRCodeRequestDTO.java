package com.example.walletmicroservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RazorpayQRCodeRequestDTO {
    private String customerId;
    private String orderId;
    private BigDecimal amount;
    private String currency = "INR";
    private String description;
    private String customerName; // Optional
    private String customerEmail; // Optional
    private String customerPhone; // Optional
}