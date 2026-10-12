package com.bank.ui;

import com.bank.model.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.List;

final class TransactionPrinter {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ROW_FORMAT = "%-8s %-17s %-26s %12s %-8s %-6s%n";

    private TransactionPrinter() {
        // utility class: no instances
    }

    static void printReceipt(Transaction tx) {
        System.out.println("Done. Reference " + tx.transactionId() + ": " + tx.type()
                + " of " + tx.amount() + " at " + WHEN.format(tx.timestamp()));
    }

    /** Prints transactions from one account's point of view: + is money in, - is money out. */
    static void printStatement(String accountNumber, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        System.out.printf(ROW_FORMAT, "Ref", "When", "Description", "Amount", "Status", "By");
        for (Transaction tx : transactions) {
            System.out.printf(ROW_FORMAT, tx.transactionId(), WHEN.format(tx.timestamp()),
                    describe(tx, accountNumber), "%+.2f".formatted(tx.amountFor(accountNumber)),
                    tx.status(), tx.performedBy());
            if (!tx.isSuccessful()) {
                System.out.println("         Reason: " + tx.note());
            }
        }
    }

    private static String describe(Transaction tx, String accountNumber) {
        return switch (tx.type()) {
            case OPENING_DEPOSIT -> "Opening deposit";
            case DEPOSIT -> "Cash deposit";
            case WITHDRAWAL -> "Cash withdrawal";
            case INTEREST -> "Monthly interest";
            case TRANSFER -> accountNumber.equals(tx.sourceAccount())
                    ? "Transfer to " + tx.destinationAccount()
                    : "Transfer from " + tx.sourceAccount();
        };
    }
}