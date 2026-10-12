package com.bank.repository;

import com.bank.model.Account;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {

    private static final String NUMBER_PREFIX = "A";

    private final Map<String, Account> accounts = new LinkedHashMap<>();

    @Override
    public void add(Account account) {
        Objects.requireNonNull(account, "Account is required");
        Account existing = accounts.putIfAbsent(account.getAccountNumber(), account);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "Account number already exists: " + account.getAccountNumber());
        }
    }

    @Override
    public void update(Account account) {
        Objects.requireNonNull(account, "Account is required");
        if (!accounts.containsKey(account.getAccountNumber())) {
            throw new IllegalArgumentException(
                    "No account with number: " + account.getAccountNumber());
        }
        accounts.put(account.getAccountNumber(), account);
    }

    @Override
    public Optional<Account> findByNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    @Override
    public List<Account> findByCustomerId(String customerId) {
        return accounts.values().stream()
                .filter(account -> account.getCustomerId().equals(customerId))
                .toList();
    }

    @Override
    public List<Account> findAll() {
        return List.copyOf(accounts.values());
    }

    @Override
    public String nextAccountNumber() {
        int highest = accounts.keySet().stream()
                .mapToInt(number -> Integer.parseInt(number.substring(NUMBER_PREFIX.length())))
                .max()
                .orElse(0);
        return "%s%05d".formatted(NUMBER_PREFIX, highest + 1);
    }
}