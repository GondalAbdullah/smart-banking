package com.bank.model;

import com.bank.exception.TransactionNotAllowedException;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CurrentAccount extends Account {

    private final BigDecimal overdraftLimit;

    public CurrentAccount(String accountNumber, String customerId, LocalDate openedDate,
                          BigDecimal balance, AccountStatus status, BigDecimal overdraftLimit) {
        super(accountNumber, customerId, openedDate, balance, status);
        this.overdraftLimit = requireNonNegative(overdraftLimit, "Overdraft limit");
    }

    public static CurrentAccount open(String accountNumber, String customerId,
                                      BigDecimal openingDeposit, BigDecimal overdraftLimit,
                                      LocalDate today) {
        return new CurrentAccount(accountNumber, customerId, today,
                validAmount(openingDeposit), AccountStatus.ACTIVE, overdraftLimit);
    }

    @Override
    protected void validateWithdrawal(BigDecimal amount, LocalDate today) {
        if (amount.compareTo(availableFunds()) > 0) {
            throw new TransactionNotAllowedException(
                    "Exceeds overdraft limit; available funds: " + availableFunds());
        }
    }

    @Override
    public BigDecimal calculateMonthlyInterest() {
        return BigDecimal.ZERO;
    }

    @Override
    public String typeLabel() {
        return "Current";
    }

    /** Balance plus overdraft: how much can still be withdrawn. */
    public BigDecimal availableFunds() {
        return getBalance().add(overdraftLimit);
    }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
}