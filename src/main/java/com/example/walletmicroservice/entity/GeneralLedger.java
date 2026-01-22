package com.example.walletmicroservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "general_ledger")
@Data
public class GeneralLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ledger_entry_id", unique = true, nullable = false, length = 100)
    private String ledgerEntryId;

    // Changed: Store ID instead of relationship
    @Column(name = "payment_transaction_id", nullable = false, length = 100)
    private String paymentTransactionId;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "order_id", length = 50)
    private String orderId;

    // Changed: Store ALL account details (no ManyToOne)
    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType; // Store as string

    @Column(name = "normal_balance", nullable = false, length = 10)
    private String normalBalance; // Store as string

    @Column(name = "ledger_type", nullable = false, length = 20)
    private String ledgerType; // Store as string

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_reversed")
    private Boolean isReversed = false;

    @Column(name = "reversal_reference", length = 100)
    private String reversalReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EntryType {
        DEBIT, CREDIT
    }
}