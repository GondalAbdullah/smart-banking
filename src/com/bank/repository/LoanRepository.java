package com.bank.repository;

import com.bank.model.Loan;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {

    void add(Loan loan);

    void update(Loan loan);

    Optional<Loan> findById(String loanId);

    List<Loan> findAll();

    List<Loan> findByCustomerId(String customerId);

    List<Loan> findByAccountNumber(String accountNumber);

    /** Returns the next free loan ID, e.g. L0001, L0002... */
    String nextId();

    /**
     * True if the account is linked to a loan that is still a liability (pending or approved).
     * A default method, so AccountService can use it without depending on LoanService.
     */
    default boolean hasOutstandingLoan(String accountNumber) {
        return findByAccountNumber(accountNumber).stream().anyMatch(Loan::isLiability);
    }
}