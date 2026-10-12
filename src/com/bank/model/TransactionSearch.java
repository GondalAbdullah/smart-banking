package com.bank.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Predicate;

/** What to look for in one account's history. A null filter means "any". */
public record TransactionSearch(String accountNumber, LocalDate from, LocalDate to,
                                TransactionType type) {

    public TransactionSearch {
        Objects.requireNonNull(accountNumber, "Account number is required");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'From' date " + from + " is after 'to' date " + to);
        }
    }

    public static TransactionSearch allFor(String accountNumber) {
        return new TransactionSearch(accountNumber, null, null, null);
    }

    /** Builds one predicate out of only the filters that were actually given. */
    public Predicate<Transaction> toPredicate() {
        Predicate<Transaction> matches = tx -> tx.involves(accountNumber);
        if (type != null) {
            matches = matches.and(tx -> tx.type() == type);
        }
        if (from != null) {
            matches = matches.and(tx -> !tx.date().isBefore(from));
        }
        if (to != null) {
            matches = matches.and(tx -> !tx.date().isAfter(to));
        }
        return matches;
    }
}