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
@Table(name = "ledgers",
        indexes = {
                @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_ledger_user_id", columnList = "user_id"),
                @Index(name = "idx_ledger_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_ledger_entry_date", columnList = "entry_date"),
                @Index(name = "idx_ledger_entry_type", columnList = "entry_type"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at"),
                @Index(name = "idx_ledger_account_code", columnList = "account_code")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ledger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 50, message = "Transaction ID must not exceed 50 characters")
    @Column(name = "transaction_id", nullable = false, length = 50)
    private String transactionId;

    @NotNull(message = "Wallet ID is required")
    @Positive(message = "Wallet ID must be positive")
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "Account code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,20}$", message = "Account code must be 3-20 uppercase alphanumeric characters")
    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must not exceed 100 characters")
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @NotNull(message = "Debit amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Debit amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Debit amount must have max 12 integer and 2 fraction digits")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Credit amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Credit amount must have max 12 integer and 2 fraction digits")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @NotNull(message = "Balance is required")
    @Digits(integer = 12, fraction = 2, message = "Balance must have max 12 integer and 2 fraction digits")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @NotNull(message = "Entry type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @NotNull(message = "Entry category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_category", nullable = false, length = 30)
    private EntryCategory entryCategory;

    @NotBlank(message = "Particulars are required")
    @Size(min = 3, max = 500, message = "Particulars must be between 3-500 characters")
    @Column(nullable = false, length = 500)
    private String particulars;

    @NotNull(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.000001", inclusive = true, message = "Exchange rate must be positive")
    @Digits(integer = 6, fraction = 6, message = "Exchange rate must have max 6 integer and 6 fraction digits")
    @Column(name = "exchange_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Base amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Base amount must have max 12 integer and 2 fraction digits")
    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @NotNull(message = "Base currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency must be 3 uppercase letters")
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "INR";

    @NotNull(message = "Is reconciled flag is required")
    @Column(name = "is_reconciled", nullable = false)
    private Boolean isReconciled = false;

    @NotNull(message = "Is reversal flag is required")
    @Column(name = "is_reversal", nullable = false)
    private Boolean isReversal = false;

    @NotNull(message = "Is adjustment flag is required")
    @Column(name = "is_adjustment", nullable = false)
    private Boolean isAdjustment = false;

    @NotNull(message = "Is system entry flag is required")
    @Column(name = "is_system_entry", nullable = false)
    private Boolean isSystemEntry = false;

    @NotNull(message = "Is manual entry flag is required")
    @Column(name = "is_manual_entry", nullable = false)
    private Boolean isManualEntry = false;

    @Size(max = 50, message = "Voucher number must not exceed 50 characters")
    @Column(name = "voucher_number", length = 50)
    private String voucherNumber;

    @Size(max = 100, message = "Cheque number must not exceed 100 characters")
    @Column(name = "cheque_number", length = 100)
    private String chequeNumber;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    @Column(length = 500)
    private String remarks;

    @Size(max = 100, message = "Created by must not exceed 100 characters")
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy = "SYSTEM";

    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @NotNull(message = "Entry date is required")
    @PastOrPresent(message = "Entry date cannot be in the future")
    @Column(name = "entry_date", nullable = false)
    private LocalDateTime entryDate;

    @NotNull(message = "Value date is required")
    @PastOrPresent(message = "Value date cannot be in the future")
    @Column(name = "value_date", nullable = false)
    private LocalDateTime valueDate;

    @NotNull(message = "Created timestamp is required")
    @PastOrPresent(message = "Created timestamp cannot be in the future")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Updated timestamp is required")
    @PastOrPresent(message = "Updated timestamp cannot be in the future")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reversal_date")
    private LocalDateTime reversalDate;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (entryDate == null) entryDate = LocalDateTime.now();
        if (valueDate == null) valueDate = LocalDateTime.now();
        if (baseAmount == null) baseAmount = debit.add(credit);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EntryType {
        DEBIT, CREDIT, OPENING_BALANCE, CLOSING_BALANCE, ADJUSTMENT
    }

    public enum EntryCategory {
        ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE,
        WALLET_BALANCE, HOLD_AMOUNT, COMMISSION, TAX,
        CHARGE, REFUND, SETTLEMENT, TRANSFER
    }

    // Helper Methods
    public static Ledger createDebitEntry(Long walletId, Long userId, String transactionId,
                                          BigDecimal amount, BigDecimal balance,
                                          String accountCode, String accountName,
                                          String particulars, EntryCategory category) {
        return Ledger.builder()
                .walletId(walletId)
                .userId(userId)
                .transactionId(transactionId)
                .accountCode(accountCode)
                .accountName(accountName)
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .balance(balance)
                .entryType(EntryType.DEBIT)
                .entryCategory(category)
                .particulars(particulars)
                .baseAmount(amount)
                .build();
    }

    public static Ledger createCreditEntry(Long walletId, Long userId, String transactionId,
                                           BigDecimal amount, BigDecimal balance,
                                           String accountCode, String accountName,
                                           String particulars, EntryCategory category) {
        return Ledger.builder()
                .walletId(walletId)
                .userId(userId)
                .transactionId(transactionId)
                .accountCode(accountCode)
                .accountName(accountName)
                .credit(amount)
                .debit(BigDecimal.ZERO)
                .balance(balance)
                .entryType(EntryType.CREDIT)
                .entryCategory(category)
                .particulars(particulars)
                .baseAmount(amount)
                .build();
    }

    public void reconcileEntry(String reconciledBy) {
        this.isReconciled = true;
        this.reconciledAt = LocalDateTime.now();
        this.remarks = "Reconciled by " + reconciledBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsReversal(String reason) {
        this.isReversal = true;
        this.reversalDate = LocalDateTime.now();
        this.remarks = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDebit() {
        return this.debit.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCredit() {
        return this.credit.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getAmount() {
        return isDebit() ? debit : credit;
    }
}