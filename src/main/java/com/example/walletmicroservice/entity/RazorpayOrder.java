package com.example.walletmicroservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "razorpay_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_razorpay_order_id", columnNames = {"order_id"}),
                @UniqueConstraint(name = "uk_razorpay_payment_id", columnNames = {"payment_id"})
        },
        indexes = {
                @Index(name = "idx_razorpay_order_id", columnList = "order_id"),
                @Index(name = "idx_razorpay_user_id", columnList = "user_id"),
                @Index(name = "idx_razorpay_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_razorpay_status", columnList = "status"),
                @Index(name = "idx_razorpay_created_at", columnList = "created_at"),
                @Index(name = "idx_razorpay_attempts", columnList = "attempts")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Razorpay order ID is required")
    @Size(max = 100, message = "Razorpay order ID must not exceed 100 characters")
    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @NotNull(message = "Transaction ID is required")
    @Size(max = 50, message = "Transaction ID must not exceed 50 characters")
    @Column(name = "transaction_id", nullable = false, length = 50)
    private String transactionId;

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Wallet ID is required")
    @Positive(message = "Wallet ID must be positive")
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", inclusive = true, message = "Amount must be at least 1.00")
    @Digits(integer = 12, fraction = 2, message = "Amount must have max 12 integer and 2 fraction digits")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Amount paid cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Amount paid must have max 12 integer and 2 fraction digits")
    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @NotNull(message = "Amount due is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Amount due cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Amount due must have max 12 integer and 2 fraction digits")
    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @NotBlank(message = "Receipt is required")
    @Size(max = 100, message = "Receipt must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String receipt;

    @NotNull(message = "Order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Size(max = 100, message = "Payment ID must not exceed 100 characters")
    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Size(max = 500, message = "Signature must not exceed 500 characters")
    @Column(length = 500)
    private String signature;

    @NotNull(message = "Attempts count is required")
    @Min(value = 0, message = "Attempts cannot be negative")
    @Max(value = 10, message = "Maximum attempts exceeded")
    @Column(nullable = false)
    private Integer attempts = 0;

    @NotNull(message = "Max attempts is required")
    @Min(value = 1, message = "Max attempts must be at least 1")
    @Max(value = 10, message = "Max attempts cannot exceed 10")
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @NotNull(message = "Partial payment flag is required")
    @Column(name = "partial_payment", nullable = false)
    private Boolean partialPayment = false;

    @NotNull(message = "First payment min amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "First payment min amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "First payment min amount must have max 12 integer and 2 fraction digits")
    @Column(name = "first_payment_min_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal firstPaymentMinAmount = BigDecimal.ZERO;

    @NotNull(message = "Expire by is required")
    @Future(message = "Expire by must be in the future")
    @Column(name = "expire_by", nullable = false)
    private LocalDateTime expireBy;

    @NotNull(message = "Auto capture flag is required")
    @Column(name = "auto_capture", nullable = false)
    private Boolean autoCapture = true;

    @NotNull(message = "Offer ID flag is required")
    @Column(name = "offer_id", nullable = false)
    private Boolean offerId = false;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @NotBlank(message = "Razorpay response is required")
    @Size(max = 4000, message = "Razorpay response must not exceed 4000 characters")
    @Column(name = "razorpay_response", nullable = false, length = 4000)
    private String razorpayResponse;

    @Size(max = 500, message = "Failure reason must not exceed 500 characters")
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Size(max = 100, message = "Error code must not exceed 100 characters")
    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Size(max = 500, message = "Error description must not exceed 500 characters")
    @Column(name = "error_description", length = 500)
    private String errorDescription;

    @Size(max = 100, message = "Payment method must not exceed 100 characters")
    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Size(max = 100, message = "Bank must not exceed 100 characters")
    @Column(name = "bank", length = 100)
    private String bank;

    @Size(max = 100, message = "Wallet must not exceed 100 characters")
    @Column(name = "wallet_provider", length = 100)
    private String walletProvider;

    @Size(max = 100, message = "VPA must not exceed 100 characters")
    @Column(name = "vpa", length = 100)
    private String vpa;

    @Size(max = 100, message = "Card ID must not exceed 100 characters")
    @Column(name = "card_id", length = 100)
    private String cardId;

    @Size(max = 100, message = "Card network must not exceed 100 characters")
    @Column(name = "card_network", length = 100)
    private String cardNetwork;

    @Size(max = 4, message = "Card last4 must not exceed 4 characters")
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Size(max = 100, message = "Card issuer must not exceed 100 characters")
    @Column(name = "card_issuer", length = 100)
    private String cardIssuer;

    @Size(max = 100, message = "Card type must not exceed 100 characters")
    @Column(name = "card_type", length = 100)
    private String cardType;

    @Size(max = 100, message = "Card category must not exceed 100 characters")
    @Column(name = "card_category", length = 100)
    private String cardCategory;

    @NotNull(message = "Is international flag is required")
    @Column(name = "is_international", nullable = false)
    private Boolean isInternational = false;

    @NotNull(message = "Is emi flag is required")
    @Column(name = "is_emi", nullable = false)
    private Boolean isEmi = false;

    @Column(name = "emi_tenure")
    private Integer emiTenure;

    @Column(name = "emi_interest_rate", precision = 5, scale = 2)
    private BigDecimal emiInterestRate;

    @Column(name = "emi_amount", precision = 12, scale = 2)
    private BigDecimal emiAmount;

    @NotNull(message = "Is recurring flag is required")
    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Size(max = 100, message = "Recurring ID must not exceed 100 characters")
    @Column(name = "recurring_id", length = 100)
    private String recurringId;

    @NotNull(message = "Is refunded flag is required")
    @Column(name = "is_refunded", nullable = false)
    private Boolean isRefunded = false;

    @NotNull(message = "Refund status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 20)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Size(max = 500, message = "Refund notes must not exceed 500 characters")
    @Column(name = "refund_notes", length = 500)
    private String refundNotes;

    @NotNull(message = "Is captured flag is required")
    @Column(name = "is_captured", nullable = false)
    private Boolean isCaptured = false;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 500, message = "User agent must not exceed 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Size(max = 1000, message = "Metadata must not exceed 1000 characters")
    @Column(length = 1000)
    private String metadata;

    @Size(max = 100, message = "Created by must not exceed 100 characters")
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy = "SYSTEM";

    @NotNull(message = "Created timestamp is required")
    @PastOrPresent(message = "Created timestamp cannot be in the future")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Updated timestamp is required")
    @PastOrPresent(message = "Updated timestamp cannot be in the future")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (amountDue == null) amountDue = amount;
        if (expireBy == null) expireBy = LocalDateTime.now().plusHours(24);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        CREATED,           // Order created
        ATTEMPTED,         // Payment attempted
        PAID,              // Payment successful
        FAILED,            // Payment failed
        EXPIRED,           // Order expired
        CANCELLED,         // Order cancelled
        REFUNDED,          // Payment refunded
        PARTIALLY_PAID,    // Partial payment
        CAPTURED           // Amount captured
    }

    public enum RefundStatus {
        NONE,              // No refund
        PENDING,           // Refund pending
        PROCESSED,         // Refund processed
        FAILED             // Refund failed
    }

    // Business Methods
    public void markAttempted() {
        this.status = OrderStatus.ATTEMPTED;
        this.attempts++;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPaid(String paymentId, String signature, BigDecimal amountPaid,
                         String paymentMethod, String bank, String walletProvider,
                         String vpa, String cardId, String cardLast4) {
        this.status = OrderStatus.PAID;
        this.paymentId = paymentId;
        this.signature = signature;
        this.amountPaid = amountPaid;
        this.amountDue = this.amount.subtract(amountPaid);
        this.paymentMethod = paymentMethod;
        this.bank = bank;
        this.walletProvider = walletProvider;
        this.vpa = vpa;
        this.cardId = cardId;
        this.cardLast4 = cardLast4;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String reason, String errorCode, String errorDescription) {
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.failedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = OrderStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markRefunded(BigDecimal refundAmount, String refundNotes) {
        this.isRefunded = true;
        this.refundStatus = RefundStatus.PROCESSED;
        this.refundAmount = refundAmount;
        this.refundNotes = refundNotes;
        this.refundedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markCaptured() {
        this.isCaptured = true;
        this.capturedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID || this.status == OrderStatus.CAPTURED;
    }

    public boolean canRetry() {
        return (this.status == OrderStatus.CREATED || this.status == OrderStatus.ATTEMPTED)
                && this.attempts < this.maxAttempts
                && LocalDateTime.now().isBefore(this.expireBy);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expireBy);
    }

    public boolean isFullyPaid() {
        return this.amountPaid.compareTo(this.amount) >= 0;
    }

    public BigDecimal getRemainingAmount() {
        return this.amount.subtract(this.amountPaid).max(BigDecimal.ZERO);
    }

    public boolean canCapture() {
        return this.isPaid() && !this.isCaptured && this.autoCapture;
    }

    public boolean canRefund() {
        return this.isPaid() && !this.isRefunded && this.paidAt != null
                && this.paidAt.isAfter(LocalDateTime.now().minusDays(90));
    }
}