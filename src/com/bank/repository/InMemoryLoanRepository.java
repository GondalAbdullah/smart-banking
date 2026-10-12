package com.bank.repository;

import com.bank.model.Loan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryLoanRepository implements LoanRepository {

    private static final String ID_PREFIX = "L";

    private final Map<String, Loan> loans = new LinkedHashMap<>();

    @Override
    public void add(Loan loan) {
        Objects.requireNonNull(loan, "Loan is required");
        if (loans.putIfAbsent(loan.getLoanId(), loan) != null) {
            throw new IllegalArgumentException("Loan ID already exists: " + loan.getLoanId());
        }
    }

    @Override
    public void update(Loan loan) {
        Objects.requireNonNull(loan, "Loan is required");
        if (!loans.containsKey(loan.getLoanId())) {
            throw new IllegalArgumentException("No loan with ID: " + loan.getLoanId());
        }
        loans.put(loan.getLoanId(), loan);
    }

    @Override
    public Optional<Loan> findById(String loanId) {
        return Optional.ofNullable(loans.get(loanId));
    }

    @Override
    public List<Loan> findAll() {
        return List.copyOf(loans.values());
    }

    @Override
    public List<Loan> findByCustomerId(String customerId) {
        return loans.values().stream()
                .filter(loan -> loan.getCustomerId().equals(customerId))
                .toList();
    }

    @Override
    public List<Loan> findByAccountNumber(String accountNumber) {
        return loans.values().stream()
                .filter(loan -> loan.getAccountNumber().equals(accountNumber))
                .toList();
    }

    @Override
    public String nextId() {
        int highest = loans.keySet().stream()
                .mapToInt(id -> Integer.parseInt(id.substring(ID_PREFIX.length())))
                .max()
                .orElse(0);
        return "%s%04d".formatted(ID_PREFIX, highest + 1);
    }
}