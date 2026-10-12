package com.bank.model;

import com.bank.exception.TransactionNotAllowedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class FixedDepositAccount extends Account {

    private final BigDecimal annualInterestRate;
    private final LocalDate maturityDate;

    public FixedDepositAccount(String accountNumber, String customerId, LocalDate openedDate,
                               BigDecimal balance, AccountStatus status,
                               BigDecimal annualInterestRate, LocalDate maturityDate) {
        super(accountNumber, customerId, openedDate, balance, status);
        this.annualInterestRate = requireNonNegative(annualInterestRate, "Interest rate");
        this.maturityDate = Objects.requireNonNull(maturityDate, "Maturity date is required");
        if (!maturityDate.isAfter(openedDate)) {
            throw new IllegalArgumentException("Maturity date must be after the opening date");
        }
    }

    public static FixedDepositAccount open(String accountNumber, String customerId,
                                           BigDecimal deposit, BigDecimal annualInterestRate,
                                           int termMonths, LocalDate today) {
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Term must be at least 1 month");
        }
        return new FixedDepositAccount(accountNumber, customerId, today, validAmount(deposit),
                AccountStatus.ACTIVE, annualInterestRate, today.plusMonths(termMonths));
    }

    @Override
    protected void validateDeposit(BigDecimal amount) {
        throw new TransactionNotAllowedException(
                "Fixed deposit accounts accept funds only when opened");
    }

    @Override
    protected void validateWithdrawal(BigDecimal amount, LocalDate today) {
        if (!isMatured(today)) {
            throw new TransactionNotAllowedException(
                    "Withdrawals are not allowed before maturity on " + maturityDate);
        }
        if (amount.compareTo(getBalance()) > 0) {
            throw new TransactionNotAllowedException("Insufficient funds; balance: " + getBalance());
        }
    }

    @Override
    public BigDecimal calculateMonthlyInterest() {
        return monthlyInterest(getBalance(), annualInterestRate);
    }

    @Override
    public String typeLabel() {
        return "Fixed Deposit";
    }

    public boolean isMatured(LocalDate today) {
        return !today.isBefore(maturityDate);
    }

    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public LocalDate getMaturityDate() { return maturityDate; }
}