package com.example.walletmicroservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_user_id", columnNames = {"user_id"})
        },
        indexes = {
                @Index(name = "idx_wallet_user_id", columnList = "user_id"),
                @Index(name = "idx_wallet_status", columnList = "status"),
                @Index(name = "idx_wallet_created_at", columnList = "created_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @NotBlank(message = "Wallet name is required")
    @Size(min = 1, max = 100, message = "Wallet name must be between 1-100 characters")
    @Column(name = "wallet_name", nullable = false, length = 100)
    private String walletName = "My Wallet";

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Balance must have max 12 integer and 2 fraction digits")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull(message = "Available balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Available balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Available balance must have max 12 integer and 2 fraction digits")
    @Column(name = "available_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @NotNull(message = "Hold balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Hold balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Hold balance must have max 12 integer and 2 fraction digits")
    @Column(name = "hold_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal holdBalance = BigDecimal.ZERO;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @NotNull(message = "Wallet status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

    @NotNull(message = "Is locked flag is required")
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @NotNull(message = "Is verified flag is required")
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @NotNull(message = "KYC status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KYCStatus kycStatus = KYCStatus.PENDING;

    @Min(value = 0, message = "Daily transaction count cannot be negative")
    @Column(name = "daily_transaction_count", nullable = false)
    private Integer dailyTransactionCount = 0;

    @DecimalMin(value = "0.00", inclusive = true, message = "Daily transaction amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Daily transaction amount must have max 12 integer and 2 fraction digits")
    @Column(name = "daily_transaction_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyTransactionAmount = BigDecimal.ZERO;

    @NotNull(message = "Daily limit is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Daily limit must be positive")
    @Digits(integer = 12, fraction = 2, message = "Daily limit must have max 12 integer and 2 fraction digits")
    @Column(name = "daily_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyLimit = new BigDecimal("50000.00");

    @NotNull(message = "Transaction limit is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Transaction limit must be positive")
    @Digits(integer = 12, fraction = 2, message = "Transaction limit must have max 12 integer and 2 fraction digits")
    @Column(name = "transaction_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal transactionLimit = new BigDecimal("10000.00");

    @NotNull(message = "Minimum balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Minimum balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Minimum balance must have max 12 integer and 2 fraction digits")
    @Column(name = "min_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal minBalance = BigDecimal.ZERO;

    @PastOrPresent(message = "Last transaction date cannot be in the future")
    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;

    @Column(name = "last_topup_date")
    private LocalDateTime lastTopupDate;

    @NotBlank(message = "Wallet version is required")
    @Column(name = "version", nullable = false, length = 10)
    private String version = "1.0";

    @NotNull(message = "Created timestamp is required")
    @PastOrPresent(message = "Created timestamp cannot be in the future")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Updated timestamp is required")
    @PastOrPresent(message = "Updated timestamp cannot be in the future")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    @Column(length = 500)
    private String remarks;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (walletName == null) walletName = "My Wallet";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum WalletStatus {
        ACTIVE, INACTIVE, FROZEN, SUSPENDED, CLOSED, PENDING_VERIFICATION
    }

    public enum KYCStatus {
        PENDING, IN_PROGRESS, VERIFIED, REJECTED, EXPIRED
    }

    // Business Methods
    public boolean canDebit(BigDecimal amount) {
        return isActive() &&
                hasSufficientBalance(amount) &&
                isWithinDailyLimit(amount) &&
                isWithinTransactionLimit(amount);
    }

    public boolean isActive() {
        return status == WalletStatus.ACTIVE && !isLocked && isVerified;
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }

    public boolean isWithinDailyLimit(BigDecimal amount) {
        return dailyTransactionAmount.add(amount).compareTo(dailyLimit) <= 0;
    }

    public boolean isWithinTransactionLimit(BigDecimal amount) {
        return amount.compareTo(transactionLimit) <= 0;
    }

    public void addHold(BigDecimal amount) {
        this.holdBalance = this.holdBalance.add(amount);
        this.availableBalance = this.balance.subtract(this.holdBalance);
        this.updatedAt = LocalDateTime.now();
    }

    public void releaseHold(BigDecimal amount) {
        this.holdBalance = this.holdBalance.subtract(amount);
        this.availableBalance = this.balance.subtract(this.holdBalance);
        this.updatedAt = LocalDateTime.now();
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.availableBalance = this.balance.subtract(this.holdBalance);
        this.lastTransactionDate = LocalDateTime.now();
        this.dailyTransactionCount++;
        this.dailyTransactionAmount = this.dailyTransactionAmount.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.balance.subtract(this.holdBalance);
        this.lastTransactionDate = LocalDateTime.now();
        this.dailyTransactionCount++;
        this.dailyTransactionAmount = this.dailyTransactionAmount.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void resetDailyCounters() {
        this.dailyTransactionCount = 0;
        this.dailyTransactionAmount = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public void markVerified() {
        this.isVerified = true;
        this.kycStatus = KYCStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void lockWallet(String reason) {
        this.isLocked = true;
        this.status = WalletStatus.FROZEN;
        this.lockedAt = LocalDateTime.now();
        this.remarks = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void unlockWallet() {
        this.isLocked = false;
        this.status = WalletStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
}