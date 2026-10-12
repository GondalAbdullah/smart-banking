package com.bank.repository;

import com.bank.model.Transaction;
import com.bank.model.TransactionSearch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InMemoryTransactionRepository implements TransactionRepository {

    private static final String ID_PREFIX = "T";

    private final Map<String, Transaction> transactions = new LinkedHashMap<>();
    private int highestNumber;

    @Override
    public void add(Transaction transaction) {
        Objects.requireNonNull(transaction, "Transaction is required");
        String id = transaction.transactionId();
        int number = Integer.parseInt(id.substring(ID_PREFIX.length()));  // parse first: fail before storing

        if (transactions.putIfAbsent(id, transaction) != null) {
            throw new IllegalArgumentException("Transaction ID already exists: " + id);
        }
        highestNumber = Math.max(highestNumber, number);
    }

    @Override
    public List<Transaction> search(TransactionSearch criteria) {
        Objects.requireNonNull(criteria, "Search criteria are required");
        return transactions.values().stream()
                .filter(criteria.toPredicate())
                .sorted(Transaction.CHRONOLOGICAL)
                .toList();
    }

    @Override
    public String nextId() {
        return "%s%06d".formatted(ID_PREFIX, highestNumber + 1);
    }
}