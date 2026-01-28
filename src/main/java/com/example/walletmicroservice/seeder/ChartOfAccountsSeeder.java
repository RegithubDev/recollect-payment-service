package com.example.walletmicroservice.seeder;

import com.example.walletmicroservice.entity.ChartOfAccounts;
import com.example.walletmicroservice.repository.ChartOfAccountsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChartOfAccountsSeeder implements CommandLineRunner {

    private final ChartOfAccountsRepository chartOfAccountsRepository;

    @Override
    public void run(String... args) {
        // Check if data already exists
        if (chartOfAccountsRepository.count() == 0) {
            List<ChartOfAccounts> accounts = Arrays.asList(
                    createAccount("1001", "(Asset) DEBIT = Bank balance increases",
                            ChartOfAccounts.AccountType.ASSET, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.REAL,
                            "Pay-In - Customer to Company online or UPI"),

                    createAccount("1002", "(Clearing) CREDIT = Clear pending amount",
                            ChartOfAccounts.AccountType.INCOME, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.REAL, "Pay-In - Customer to Company online or UPI"),

                    createAccount("1003", "(Income) DEBIT = Reduce revenue",
                            ChartOfAccounts.AccountType.INCOME, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.REAL,
                            "Refund request from customer for above pay-in"),

                    createAccount("1004", "(Clearing) CREDIT = Move to pending",
                            ChartOfAccounts.AccountType.CLEARING, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.REAL, "Refund credit to customer account from company based on request"),

                    createAccount("1005", "(Clearing) DEBIT = Clear pending",
                            ChartOfAccounts.AccountType.CLEARING, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.REAL, "Refund payout on success"),

                    createAccount("1006", "(Asset) CREDIT = Bank balance decreases",
                            ChartOfAccounts.AccountType.ASSET, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.REAL, "Refund payout on success"),

                    createAccount("1007", "(Clearing) DEBIT = Clear pending",
                            ChartOfAccounts.AccountType.CLEARING, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.REAL, "Refund payout on failure"),

                    createAccount("1008", "(Income) CREDIT = Increase in revenue",
                            ChartOfAccounts.AccountType.INCOME, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.REAL, "Refund payout on failure"),

                    createAccount("1009", "(Expense) DEBIT = Increase expense",
                            ChartOfAccounts.AccountType.EXPENSE, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.REAL, "Pay-out from company to customer wallet"),

                    createAccount("1010", "(Liability) CREDIT = Create wallet liability",
                            ChartOfAccounts.AccountType.LIABILITY, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.WALLET, "Pay-out from company to customer wallet"),

                    createAccount("1011", "(Liability) DEBIT = Reduce wallet liability",
                            ChartOfAccounts.AccountType.LIABILITY, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.WALLET,
                            "Approved withdrawal request from wallet - Customer Bank"),

                    createAccount("1012", "(Clearing) CREDIT = Move to pending",
                            ChartOfAccounts.AccountType.CLEARING, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.REAL, "Approved withdrawal request from wallet - Customer Bank"),

                    createAccount("1013", "(Clearing) DEBIT = Clear pending",
                            ChartOfAccounts.AccountType.CLEARING, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.REAL, "Success withdrawal request from wallet - Customer Bank"),

                    createAccount("1014", "(Asset) CREDIT = Bank balance decreases",
                            ChartOfAccounts.AccountType.ASSET, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.REAL, "Success withdrawal request from wallet - Customer Bank"),

                    createAccount("1015", "(Clearing) DEBIT = Clear pending",
                            ChartOfAccounts.AccountType.CLEARING, ChartOfAccounts.NormalBalance.DEBIT,
                            ChartOfAccounts.LedgerType.REAL, "Failure withdrawal request from wallet - Customer Bank"),

                    createAccount("1016", "(Liability) CREDIT = Return to wallet liability",
                            ChartOfAccounts.AccountType.LIABILITY, ChartOfAccounts.NormalBalance.CREDIT,
                            ChartOfAccounts.LedgerType.WALLET, "Failure withdrawal request from wallet - Customer Bank")
            );

            chartOfAccountsRepository.saveAll(accounts);
            System.out.println("Chart of Accounts seeded successfully!");
        }
    }

    private ChartOfAccounts createAccount(String accountId, String accountName,
        ChartOfAccounts.AccountType accountType,ChartOfAccounts.NormalBalance normalBalance,
                                          ChartOfAccounts.LedgerType ledgerType,String description) {
        ChartOfAccounts account = new ChartOfAccounts();
        account.setAccountId(accountId);
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        account.setNormalBalance(normalBalance);
        account.setLedgerType(ledgerType);
        account.setDescription(description);
        account.setIsActive(true);
        return account;
    }
}