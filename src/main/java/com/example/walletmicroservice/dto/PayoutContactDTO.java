package com.example.walletmicroservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayoutContactDTO {

    public Long id;

    public String contactId;

    public String customerId;

    public String name;

    public String email;

    public String mobile;

    public String type;

    public String referenceId;

    public String razorpayContactId;

    public String status; // active, inactive

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;
}