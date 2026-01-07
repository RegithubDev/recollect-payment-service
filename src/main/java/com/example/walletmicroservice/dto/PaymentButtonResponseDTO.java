package com.example.walletmicroservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment link response")
public class PaymentButtonResponseDTO {

    @Schema(description = "Payment link ID", example = "plink_1234567890")
    private String paymentLinkId;

    @Schema(description = "Short URL", example = "https://rzp.io/i/XXXXXX")
    private String shortUrl;

    @Schema(description = "Full URL", example = "https://rzp.io/l/XXXXXX")
    private String url;

    @Schema(description = "Amount in rupees", example = "123.45")
    private String amount;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Description", example = "Payment for Invoice #123")
    private String description;

    @Schema(description = "Status", example = "created")
    private String status;

    @Schema(description = "Created timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Expiry timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt; // UNCOMMENT THIS!

    @Schema(description = "Embed HTML code for payment link")
    private String embedHtml;

    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;
}