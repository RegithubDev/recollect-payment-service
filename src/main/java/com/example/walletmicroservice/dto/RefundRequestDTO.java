package com.example.walletmicroservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundRequestDTO {
    // Original fields
    private String paymentTransactionId;  // Original payment transaction ID
    private BigDecimal amount;             // Refund amount
    private String currency = "INR";       // Currency (default INR)
    private String reason;                 // Reason for refund
    private String notes;                  // Additional notes

    // New fields for approval workflow
    private String requesterId;            // User ID requesting the refund
    private String requesterRole;          // Role of requester
    private String customerId;             // Customer ID (for validation)
    private LocalDateTime requestDate = LocalDateTime.now();

    // Optional: Partial refund tracking
    private Boolean isPartialRefund = false;
    private BigDecimal originalAmount;     // Original payment amount
}