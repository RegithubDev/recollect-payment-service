package com.example.walletmicroservice.dto;

import lombok.Data;

@Data
public class FundAccountRequestDTO {
    private String customerId;
    private String contactId; // Razorpay contact ID
    private String accountType; // bank_account, vpa, card

    // For bank account
    private String accountNumber;
    private String ifscCode;
    private String accountHolderName;

    // For UPI
    private String vpaAddress;
}