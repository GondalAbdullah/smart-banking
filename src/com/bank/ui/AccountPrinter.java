package com.bank.ui;

import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.FixedDepositAccount;
import com.bank.model.SavingsAccount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class AccountPrinter {

    private static final String ROW_FORMAT = "%-8s %-14s %-8s %14s %-8s%n";

    private AccountPrinter() {
        // utility class: no instances
    }

    static void printTable(List<Account> accounts) {
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        System.out.printf(ROW_FORMAT, "Number", "Type", "Customer", "Balance", "Status");
        accounts.forEach(a -> System.out.printf(ROW_FORMAT,
                a.getAccountNumber(), a.typeLabel(), a.getCustomerId(),
                a.getBalance(), a.getStatus()));
    }

    static void printDetails(Account account, LocalDate today) {
        System.out.print("""
                Account:   %s
                Type:      %s
                Customer:  %s
                Opened:    %s
                Balance:   %s
                Status:    %s
                """.formatted(account.getAccountNumber(), account.typeLabel(),
                account.getCustomerId(), account.getOpenedDate(),
                account.getBalance(), account.getStatus()));
        System.out.println(typeSpecificDetails(account, today));
    }

    private static String typeSpecificDetails(Account account, LocalDate today) {
        return switch (account) {
            case SavingsAccount s -> "Minimum balance: %s | Interest: %s | Withdrawals left this month: %d"
                    .formatted(s.getMinimumBalance(), percent(s.getAnnualInterestRate()),
                            s.remainingWithdrawals(today));
            case CurrentAccount c -> "Overdraft limit: %s | Available funds: %s"
                    .formatted(c.getOverdraftLimit(), c.availableFunds());
            case FixedDepositAccount f -> "Interest: %s | Matures: %s%s"
                    .formatted(percent(f.getAnnualInterestRate()), f.getMaturityDate(),
                            f.isMatured(today) ? " (matured)" : "");
        };
    }

    private static String percent(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}