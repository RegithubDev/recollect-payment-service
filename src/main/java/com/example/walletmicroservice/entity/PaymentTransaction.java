package com.example.walletmicroservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 100)
    private String transactionId;

    @Column(name = "razorpay_payment_id", unique = true, length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "razorpay_payout_id", length = 100)
    private String razorpayPayoutId;

    @Column(name = "razorpay_contact_id", length = 100)
    private String razorpayContactId;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "payout_id", length = 50)
    private String payoutId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 30)
    private RefundStatus refundStatus;

    @Column(name = "refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @Column(name = "refund_approved_by", length = 50)
    private String refundApprovedBy;

    @Column(name = "refund_approved_at")
    private LocalDateTime refundApprovedAt;

    @Column(name = "refund_approval_remark", columnDefinition = "TEXT")
    private String refundApprovalRemark;

    @Column(name = "contact_verification_status", length = 30)
    private String contactVerificationStatus;

    @Column(name = "is_wallet_transfer")
    private Boolean isWalletTransfer = false;

    @Column(columnDefinition = "JSON")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TransactionType {
        PAYIN, PAYOUT, REFUND, WALLET_TRANSFER, WITHDRAWAL
    }

    public enum PaymentMethod {
        REAL, WALLET, UPI, CARD, NETBANKING
    }

    public enum TransactionStatus {
        CAPTURED, CREATED ,AUTHORIZED, FAILED, PROCESSING
    }

    public enum RefundStatus {
        REQUESTED, APPROVED, PROCESSED, FAILED
    }
}