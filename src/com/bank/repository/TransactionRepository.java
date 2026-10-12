package com.bank.repository;

import com.bank.model.Transaction;
import com.bank.model.TransactionSearch;

import java.util.List;

/** Append-only: there is deliberately no update or delete. */
public interface TransactionRepository {

    void add(Transaction transaction);

    /** Matching transactions, oldest first. */
    List<Transaction> search(TransactionSearch criteria);

    /** Returns the next free transaction ID, e.g. T000001, T000002... */
    String nextId();
}