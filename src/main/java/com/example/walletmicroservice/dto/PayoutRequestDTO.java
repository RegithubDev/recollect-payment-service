package com.example.walletmicroservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class PayoutRequestDTO {
    private String customerId;
    private String contactId; // Your internal contact ID
    private String fundAccountId; // Razorpay fund account ID
    private BigDecimal amount;
    private String currency = "INR";
    private String mode = "NEFT"; // NEFT, IMPS, RTGS, UPI
    private String purpose = "payout";
    private String referenceId;
    private String narration;
    private Map<String, Object> notes;
}