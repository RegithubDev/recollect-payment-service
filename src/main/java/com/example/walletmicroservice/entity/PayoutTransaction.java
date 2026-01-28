package com.example.walletmicroservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_transactions")
@Data
public class PayoutTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payout_id", unique = true, nullable = false)
    private String payoutId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "contact_id", nullable = false)
    private String contactId;

    @Column(name = "fund_account_id", nullable = false)
    private String fundAccountId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Column(name = "mode", length = 20) // NEFT, IMPS, RTGS, UPI, card
    private String mode;

    @Column(name = "purpose", length = 50) // refund, cashback, withdrawal, salary
    private String purpose;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "narration")
    private String narration;

    @Column(name = "razorpay_payout_id")
    private String razorpayPayoutId;

    @Column(name = "status", length = 30)
    private String status; // created, processing, processed, failed, rejected

    @Column(name = "utr_number")
    private String utrNumber;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "fees", precision = 10, scale = 2)
    private BigDecimal fees;

    @Column(name = "tax", precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_uid", updatable = false)
    private String createdUid;

    @Column(name = "updated_uid")
    private String updatedUid;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}