package com.example.walletmicroservice.service;

import com.example.walletmicroservice.entity.ChartOfAccounts;
import com.example.walletmicroservice.entity.GeneralLedger;
import com.example.walletmicroservice.repository.ChartOfAccountsRepository;
import com.example.walletmicroservice.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerHelperService {

    private final ChartOfAccountsRepository chartOfAccountsRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    /**
     * Create double entry for PAYIN (payment captured)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPayInLedgerEntries(String paymentTransactionId,
                                         String transactionId,
                                         String customerId,
                                         String orderId,
                                         BigDecimal amount,
                                         String description) {

        // Get account details from master table
        ChartOfAccounts companyBankAccount = chartOfAccountsRepository.findById("Company Bank A/C")
                .orElseThrow(() -> new RuntimeException("Account not found: Company Bank A/C"));

        ChartOfAccounts salesRevenueAccount = chartOfAccountsRepository.findById("Sales Revenue")
                .orElseThrow(() -> new RuntimeException("Account not found: Sales Revenue"));

        // Generate unique reference
        String ledgerRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8);

        // Create Debit Entry (Company Bank A/C)
        GeneralLedger debitEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Payment received - " + description,
                companyBankAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Create Credit Entry (Sales Revenue)
        GeneralLedger creditEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Sales revenue - " + description,
                salesRevenueAccount,
                GeneralLedger.EntryType.CREDIT
        );

        // Save entries
        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created PAYIN ledger entries for transaction: {}", transactionId);
    }

    /**
     * Create double entry for REFUND created
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createRefundLedgerEntries(String paymentTransactionId,
                                          String transactionId,
                                          String customerId,
                                          String orderId,
                                          BigDecimal amount,
                                          String description) {

        ChartOfAccounts salesRevenueAccount = chartOfAccountsRepository.findById("Sales Revenue")
                .orElseThrow(() -> new RuntimeException("Account not found: Sales Revenue"));

        ChartOfAccounts refundPayableAccount = chartOfAccountsRepository.findById("Customer Refund Payable")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Refund Payable"));

        String ledgerRef = "RFN-" + UUID.randomUUID().toString().substring(0, 8);

        // Debit Sales Revenue (Reversal)
        GeneralLedger debitEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Refund issued - " + description,
                salesRevenueAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Customer Refund Payable
        GeneralLedger creditEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Refund payable - " + description,
                refundPayableAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created REFUND ledger entries for transaction: {}", transactionId);
    }

    /**
     * Create double entry for REFUND payout (when refund is processed)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createRefundPayoutLedgerEntries(String paymentTransactionId,
                                                String transactionId,
                                                String customerId,
                                                String orderId,
                                                BigDecimal amount,
                                                String description) {

        ChartOfAccounts refundPayableAccount = chartOfAccountsRepository.findById("Customer Refund Payable")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Refund Payable"));

        ChartOfAccounts companyBankAccount = chartOfAccountsRepository.findById("Company Bank A/C")
                .orElseThrow(() -> new RuntimeException("Account not found: Company Bank A/C"));

        String ledgerRef = "RFP-" + UUID.randomUUID().toString().substring(0, 8);

        // Debit Customer Refund Payable
        GeneralLedger debitEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Refund payout - " + description,
                refundPayableAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Company Bank A/C
        GeneralLedger creditEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Bank transfer for refund - " + description,
                companyBankAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created REFUND PAYOUT ledger entries for transaction: {}", transactionId);
    }

    /**
     * Create double entry for WALLET TRANSFER
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createWalletTransferLedgerEntries(String paymentTransactionId,
                                                  String transactionId,
                                                  String customerId,
                                                  String orderId,
                                                  BigDecimal amount,
                                                  String description) {

        ChartOfAccounts payoutExpensesAccount = chartOfAccountsRepository.findById("Customer Payout Expenses")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Payout Expenses"));

        ChartOfAccounts walletLiabilityAccount = chartOfAccountsRepository.findById("Customer Wallet Liability")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Wallet Liability"));

        String ledgerRef = "WLT-" + UUID.randomUUID().toString().substring(0, 8);

        // Debit Customer Payout Expenses
        GeneralLedger debitEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Wallet expense - " + description,
                payoutExpensesAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Customer Wallet Liability
        GeneralLedger creditEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Wallet liability - " + description,
                walletLiabilityAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created WALLET TRANSFER ledger entries for transaction: {}", transactionId);
    }

    /**
     * Create double entry for WALLET PAYOUT (withdrawal)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createWalletPayoutLedgerEntries(String paymentTransactionId,
                                                String transactionId,
                                                String customerId,
                                                String orderId,
                                                BigDecimal amount,
                                                String description) {

        ChartOfAccounts walletLiabilityAccount = chartOfAccountsRepository.findById("Customer Wallet Liability")
                .orElseThrow(() -> new RuntimeException("Account not found: Customer Wallet Liability"));

        ChartOfAccounts payoutClearingAccount = chartOfAccountsRepository.findById("Payout Pending / Clearing")
                .orElseThrow(() -> new RuntimeException("Account not found: Payout Pending / Clearing"));

        String ledgerRef = "WPO-" + UUID.randomUUID().toString().substring(0, 8);

        // Debit Customer Wallet Liability
        GeneralLedger debitEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Wallet payout - " + description,
                walletLiabilityAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Payout Pending / Clearing
        GeneralLedger creditEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Payout clearing - " + description,
                payoutClearingAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created WALLET PAYOUT ledger entries for transaction: {}", transactionId);
    }

    /**
     * Create double entry for PAYOUT PROCESSED (bank transfer)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPayoutProcessedLedgerEntries(String paymentTransactionId,
                                                   String transactionId,
                                                   String customerId,
                                                   String orderId,
                                                   BigDecimal amount,
                                                   String description) {

        ChartOfAccounts payoutClearingAccount = chartOfAccountsRepository.findById("Payout Pending / Clearing")
                .orElseThrow(() -> new RuntimeException("Account not found: Payout Pending / Clearing"));

        ChartOfAccounts companyBankAccount = chartOfAccountsRepository.findById("Company Bank A/C")
                .orElseThrow(() -> new RuntimeException("Account not found: Company Bank A/C"));

        String ledgerRef = "POP-" + UUID.randomUUID().toString().substring(0, 8);

        // Debit Payout Pending / Clearing
        GeneralLedger debitEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Payout processed - " + description,
                payoutClearingAccount,
                GeneralLedger.EntryType.DEBIT
        );

        // Credit Company Bank A/C
        GeneralLedger creditEntry = createLedgerEntry(
                paymentTransactionId,
                transactionId,
                ledgerRef,
                customerId,
                orderId,
                amount,
                "Bank payout - " + description,
                companyBankAccount,
                GeneralLedger.EntryType.CREDIT
        );

        generalLedgerRepository.save(debitEntry);
        generalLedgerRepository.save(creditEntry);

        log.info("Created PAYOUT PROCESSED ledger entries for transaction: {}", transactionId);
    }

    /**
     * Helper method to create ledger entry with copied account values
     */
    private GeneralLedger createLedgerEntry(String paymentTransactionId,
                                            String transactionId,
                                            String ledgerRef,
                                            String customerId,
                                            String orderId,
                                            BigDecimal amount,
                                            String description,
                                            ChartOfAccounts account,
                                            GeneralLedger.EntryType entryType) {

        GeneralLedger entry = new GeneralLedger();
        entry.setLedgerEntryId(generateLedgerEntryId(entryType));
        entry.setPaymentTransactionId(paymentTransactionId);
        entry.setTransactionId(transactionId);
        entry.setEntryDate(LocalDate.now());
        entry.setCustomerId(customerId);
        entry.setOrderId(orderId);

        // Copy ALL values from ChartOfAccounts (no relationship)
        entry.setAccountId(account.getAccountId());
        entry.setAccountName(account.getAccountName());
        entry.setAccountType(account.getAccountType().name());
        entry.setNormalBalance(account.getNormalBalance().name());
        entry.setLedgerType(account.getLedgerType().name());

        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setDescription(description);
        entry.setIsReversed(false);

        return entry;
    }

    private String generateLedgerEntryId(GeneralLedger.EntryType entryType) {
        String prefix = entryType == GeneralLedger.EntryType.DEBIT ? "DEB" : "CRED";
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}