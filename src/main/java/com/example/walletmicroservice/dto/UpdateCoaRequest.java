package com.example.walletmicroservice.dto;

import lombok.Data;

@Data
public class UpdateCoaRequest {
    private String description;
    private Boolean isActive;
}
