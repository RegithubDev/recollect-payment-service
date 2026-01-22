package com.example.walletmicroservice.dto;

import lombok.Data;

@Data
public class WebhookDTO {
    private String event;
    private String entity;
    private String accountId;
    private String paymentId;
    private String orderId;
    private String refundId;
    private String payoutId;
    private WebhookPayload payload;

    @Data
    public static class WebhookPayload {
        private Payment payment;
        private Refund refund;
        private Payout payout;

        @Data
        public static class Payment {
            private String id;
            private String entity;
            private Integer amount;
            private String currency;
            private String status;
            private String orderId;
            private String method;
            private String description;
            private Long createdAt;
            private String customerId;
            private String bank;
            private String wallet;
            private String vpa;
            private String email;
            private String contact;
        }

        @Data
        public static class Refund {
            private String id;
            private String entity;
            private Integer amount;
            private String currency;
            private String paymentId;
            private String status;
            private String speedRequested;
            private Long createdAt;
        }

        @Data
        public static class Payout {
            private String id;
            private String entity;
            private String fundAccountId;
            private Integer amount;
            private String currency;
            private String status;
            private String purpose;
            private String mode;
            private String referenceId;
            private Long createdAt;
        }
    }
}