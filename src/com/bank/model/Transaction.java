package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

/**
 * One money movement. Immutable: a mistake is corrected by recording a new
 * transaction, never by editing an old one.
 *
 * @param sourceAccount      where the money left; null for deposits and interest
 * @param destinationAccount where the money arrived; null for withdrawals
 * @param note               why a FAILED transaction was refused; null on success
 */
public record Transaction(
        String transactionId,
        TransactionType type,
        String sourceAccount,
        String destinationAccount,
        BigDecimal amount,
        LocalDateTime timestamp,
        TransactionStatus status,
        String note) {

    /** Oldest first; the ID breaks ties between transactions in the same instant. */
    public static final Comparator<Transaction> CHRONOLOGICAL =
            Comparator.comparing(Transaction::timestamp)
                    .thenComparing(Transaction::transactionId);

    public Transaction {
        Objects.requireNonNull(transactionId, "Transaction ID is required");
        Objects.requireNonNull(type, "Type is required");
        Objects.requireNonNull(timestamp, "Timestamp is required");
        Objects.requireNonNull(status, "Status is required");
        amount = Account.validAmount(amount);   // protected also means "same package"
        type.checkSides(sourceAccount, destinationAccount);
        if (sourceAccount != null && sourceAccount.equals(destinationAccount)) {
            throw new IllegalArgumentException("Source and destination must be different accounts");
        }
        if (status == TransactionStatus.FAILED && (note == null || note.isBlank())) {
            throw new IllegalArgumentException("A failed transaction must record why it failed");
        }
    }

    public boolean involves(String accountNumber) {
        return accountNumber.equals(sourceAccount) || accountNumber.equals(destinationAccount);
    }

    /** The amount from one account's point of view: positive = money in, negative = money out. */
    public BigDecimal amountFor(String accountNumber) {
        if (accountNumber.equals(destinationAccount)) {
            return amount;
        }
        if (accountNumber.equals(sourceAccount)) {
            return amount.negate();
        }
        throw new IllegalArgumentException(transactionId + " does not involve account " + accountNumber);
    }

    public LocalDate date() {
        return timestamp.toLocalDate();
    }

    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }
}