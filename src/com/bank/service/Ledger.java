package com.bank.service;

import com.bank.model.Transaction;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The single place transactions are created: it stamps each one with the next ID and the time.
 * The record methods are package-private, so menus can read history but only services can write it.
 */
public class Ledger {

    private final TransactionRepository repository;
    private final Session session;
    private final Clock clock;

    public Ledger(TransactionRepository repository, Session session, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.session = Objects.requireNonNull(session);
        this.clock = Objects.requireNonNull(clock);
    }

    Transaction recordSuccess(TransactionType type, String source, String destination,
                              BigDecimal amount) {
        return record(type, source, destination, amount, TransactionStatus.SUCCESS, null);
    }

    Transaction recordFailure(TransactionType type, String source, String destination,
                              BigDecimal amount, String reason) {
        return record(type, source, destination, amount, TransactionStatus.FAILED, reason);
    }

    private Transaction record(TransactionType type, String source, String destination,
                               BigDecimal amount, TransactionStatus status, String note) {
        Transaction transaction = new Transaction(repository.nextId(), type, source, destination,
                amount, LocalDateTime.now(clock), status, note,
                session.employee().getEmployeeId());
        repository.add(transaction);
        return transaction;
    }
}