package com.example.walletmicroservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RazorpayQRCodeResponseDTO {
    private String transactionId;
    private String razorpayOrderId;
    private String razorpayQRCodeId;
    private String qrCodeImageBase64; // QR code image in base64
    private String qrCodeImageUrl; // URL to download QR code
    private String qrShortUrl; // Short URL for the QR code
    private String upiLink; // UPI deep link
    private String paymentUrl; // Payment page URL
    private BigDecimal amount;
    private String currency;
    private String customerId;
    private String orderId;
    private String status;
    private Long createdAt;
    private String qrStatus; // active, processed, deactivated
}