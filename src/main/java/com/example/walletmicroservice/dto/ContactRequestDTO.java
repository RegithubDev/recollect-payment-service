package com.example.walletmicroservice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ContactRequestDTO {
    private String customerId;
    private String name;
    private String email;
    private String mobile;
    private String type = "customer";
    private String referenceId;
    private Map<String, Object> notes;
}