package com.example.walletmicroservice.dto;

import lombok.Data;
import com.example.walletmicroservice.entity.PaymentTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundResponseDTO {
    private String refundRequestId;        // Internal refund transaction ID
    private String razorpayRefundId;       // Razorpay refund ID (if processed)
    private String originalTransactionId;  // Original payment transaction ID
    private String customerId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;                 // PENDING, APPROVED, REJECTED, PROCESSED
    private String refundStatus;           // PENDING, PROCESSED
    private String approvalStatus;         // PENDING, APPROVED, REJECTED, PROCESSED
    private String reason;
    private String requesterId;
    private LocalDateTime requestedAt;
    private String approverId;
    private LocalDateTime approvedAt;
    private String approvalComments;
    private LocalDateTime processedAt;
    private String metadata;               // JSON metadata

    public static RefundResponseDTO fromTransaction(PaymentTransaction transaction) {
        RefundResponseDTO response = new RefundResponseDTO();
        response.setRefundRequestId(transaction.getTransactionId());
        response.setRazorpayRefundId(transaction.getRazorpayRefundId());
        response.setOriginalTransactionId(transaction.getRazorpayPaymentId()); // Adjust as needed
        response.setCustomerId(transaction.getCustomerId());
        response.setOrderId(transaction.getOrderId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setStatus(transaction.getStatus() != null ? transaction.getStatus().name() : null);
        response.setRefundStatus(transaction.getRefundStatus() != null ? transaction.getRefundStatus().name() : null);
        response.setApprovalStatus(transaction.getRefundApprovalStatus() != null ?
                transaction.getRefundApprovalStatus().name() : null);
        response.setReason(transaction.getRefundReason());
        response.setRequesterId(transaction.getRefundRequesterId());
        response.setRequestedAt(transaction.getRefundRequestedAt());
        response.setApproverId(transaction.getRefundApproverId());
        response.setApprovedAt(transaction.getRefundApprovedAt());
        response.setApprovalComments(transaction.getApprovalComments());
        response.setProcessedAt(transaction.getRefundProcessedAt());
        return response;
    }
}