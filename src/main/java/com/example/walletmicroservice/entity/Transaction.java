package com.example.walletmicroservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transaction_id", columnNames = {"transaction_id"}),
                @UniqueConstraint(name = "uk_razorpay_payment_id", columnNames = {"razorpay_payment_id"})
        },
        indexes = {
                @Index(name = "idx_txn_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_txn_user_id", columnList = "user_id"),
                @Index(name = "idx_txn_type_status", columnList = "type, status"),
                @Index(name = "idx_txn_created_at", columnList = "created_at"),
                @Index(name = "idx_txn_razorpay_order_id", columnList = "razorpay_order_id"),
                @Index(name = "idx_txn_reference_id", columnList = "reference_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Wallet ID is required")
    @Column(name = "wallet_id", nullable = false)
    @Builder.Default
    private Long walletId = 0L; // Default to 0 or -1

    @Column(name = "merchant_id")
    private Long merchantId;

    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Size(max = 50, message = "Invoice number must not exceed 50 characters")
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Size(max = 50, message = "Transaction channel must not exceed 50 characters")
    @Column(name = "transaction_channel", length = 50)
    private String transactionChannel;

    @Size(max = 50, message = "Device type must not exceed 50 characters")
    @Column(name = "device_type", length = 50)
    private String deviceType;

    @NotBlank(message = "Transaction ID is required")
    @Size(min = 10, max = 50, message = "Transaction ID must be between 10-50 characters")
    @Column(name = "transaction_id", nullable = false, unique = true, length = 50)
    private String transactionId;

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be at least 0.01")
    @Digits(integer = 12, fraction = 2, message = "Amount must have max 12 integer and 2 fraction digits")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Is payment request flag is required")
    @Column(name = "is_payment_request", nullable = false)
    @Builder.Default
    private Boolean isPaymentRequest = false;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @NotNull(message = "Transaction status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Source type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @NotBlank(message = "Source reference is required")
    @Size(max = 100, message = "Source reference must not exceed 100 characters")
    @Column(name = "source_reference", nullable = false, length = 100)
    private String sourceReference;

    @NotBlank(message = "Description is required")
    @Size(min = 3, max = 500, message = "Description must be between 3-500 characters")
    @Column(nullable = false, length = 500)
    private String description;

    @NotNull(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Fee amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Fee amount must have max 12 integer and 2 fraction digits")
    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @NotNull(message = "Tax amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Tax amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Tax amount must have max 12 integer and 2 fraction digits")
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull(message = "Net amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Net amount must be at least 0.01")
    @Digits(integer = 12, fraction = 2, message = "Net amount must have max 12 integer and 2 fraction digits")
    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @NotBlank(message = "Transaction mode is required")
    @Size(max = 20, message = "Transaction mode must not exceed 20 characters")
    @Column(name = "transaction_mode", nullable = false, length = 20)
    private String transactionMode = "ONLINE";

    @Size(max = 100, message = "Razorpay order ID must not exceed 100 characters")
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Size(max = 100, message = "Razorpay payment ID must not exceed 100 characters")
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Size(max = 100, message = "Reference ID must not exceed 100 characters")
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Size(max = 100, message = "Parent transaction ID must not exceed 100 characters")
    @Column(name = "parent_transaction_id", length = 100)
    private String parentTransactionId;

    @NotNull(message = "Is refund flag is required")
    @Column(name = "is_refund", nullable = false)
    private Boolean isRefund = false;

    @NotNull(message = "Is disputed flag is required")
    @Column(name = "is_disputed", nullable = false)
    private Boolean isDisputed = false;

    @NotNull(message = "Is reversed flag is required")
    @Column(name = "is_reversed", nullable = false)
    private Boolean isReversed = false;

    @NotNull(message = "Is settlement flag is required")
    @Column(name = "is_settlement", nullable = false)
    private Boolean isSettlement = false;

    @NotNull(message = "Requires approval flag is required")
    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    @NotNull(message = "Is approved flag is required")
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = true;

    @NotNull(message = "Is processed flag is required")
    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @NotNull(message = "Is reconciled flag is required")
    @Column(name = "is_reconciled", nullable = false)
    private Boolean isReconciled = false;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 500, message = "User agent must not exceed 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Size(max = 1000, message = "Metadata must not exceed 1000 characters")
    @Column(length = 1000)
    private String metadata;

    @Size(max = 500, message = "Failure reason must not exceed 500 characters")
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @NotNull(message = "Created timestamp is required")
    @PastOrPresent(message = "Created timestamp cannot be in the future")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Opening balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Opening balance must have max 12 integer and 2 fraction digits")
    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @NotNull(message = "Closing balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Closing balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Closing balance must have max 12 integer and 2 fraction digits")
    @Column(name = "closing_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @NotNull(message = "Updated timestamp is required")
    @PastOrPresent(message = "Updated timestamp cannot be in the future")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (transactionId == null) {
            transactionId = generateTransactionId();
        }
        if (netAmount == null) {
            netAmount = amount.subtract(feeAmount).subtract(taxAmount);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateTransactionId() {
        return "TXN" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int) (Math.random() * 10000));
    }

    public enum TransactionType {
        WALLET_TOPUP,            // Add money to wallet
        WALLET_TO_WALLET,        // Transfer to another wallet
        PAYMENT,                 // Payment to merchant
        REFUND,                  // Refund from merchant
        CASHBACK,                // System cashback
        WITHDRAWAL,              // Withdraw to bank
        DEPOSIT,                 // Direct deposit
        CHARGE,                  // Service charge
        REVERSAL,                // Transaction reversal
        ADJUSTMENT,              // Manual adjustment
        SETTLEMENT,              // Settlement transaction
        COMMISSION,              // Commission earned
        PENALTY                  // Penalty charged
    }

    public enum TransactionStatus {
        INITIATED,              // Transaction created
        PENDING,                // Waiting for processing
        PROCESSING,             // Being processed
        COMPLETED,              // Successfully completed
        FAILED,                 // Failed permanently
        CANCELLED,              // Cancelled by user
        DECLINED,               // Declined by system
        ON_HOLD,                // On hold for review
        REFUNDED,               // Refunded to user
        REVERSED,               // Reversed
        SETTLED                 // Settlement completed
    }

    public enum PaymentMethod {
        WALLET,                 // Wallet balance
        RAZORPAY,               // Razorpay gateway
        CARD,                   // Credit/Debit card
        UPI,                    // UPI payment
        NETBANKING,             // Net banking
        CASH,                   // Cash payment
        BANK_TRANSFER,          // Bank transfer
        CHEQUE,                 // Cheque payment
        EMI,                    // EMI payment
        REWARD_POINTS,          // Reward points
        VOUCHER,                // Voucher redemption
        CRYPTO                  // Cryptocurrency
    }

    public enum SourceType {
        RAZORPAY,               // Razorpay payment
        INTERNAL_TRANSFER,      // Internal transfer
        MANUAL_ADJUSTMENT,      // Manual adjustment
        CASHBACK,               // Cashback
        REFUND,                 // Refund
        CHARGE,                 // Service charge
        SETTLEMENT,             // Settlement
        SYSTEM                  // System generated
    }

    // Business Methods
    public void markCompleted() {  // Remove BigDecimal parameter
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markProcessing() {
        this.status = TransactionStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markRefunded() {
        this.status = TransactionStatus.REFUNDED;
        this.isRefund = true;
        this.refundedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markReversed() {
        this.status = TransactionStatus.REVERSED;
        this.isReversed = true;
        this.reversedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markSettled() {
        this.status = TransactionStatus.SETTLED;
        this.isSettlement = true;
        this.settlementDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(String approvedByUser) {
        this.isApproved = true;
        this.approvedBy = approvedByUser;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reconcile() {
        this.isReconciled = true;
        this.reconciledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canBeRefunded() {
        return this.status == TransactionStatus.COMPLETED &&
                !this.isRefund &&
                !this.isReversed &&
                this.createdAt.isAfter(LocalDateTime.now().minusDays(90));
    }

    public boolean isSuccess() {
        return this.status == TransactionStatus.COMPLETED ||
                this.status == TransactionStatus.SETTLED;
    }

    public void setReconciled(boolean reconciled) {
        this.isReconciled = reconciled;
        if (reconciled) {
            this.reconciledAt = LocalDateTime.now();
        }
    }

    public void setProcessed(boolean processed) {
        this.isProcessed = processed;
        if (processed) {
            this.processedAt = LocalDateTime.now();
        }
    }

}