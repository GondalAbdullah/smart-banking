package com.bank.config;

import java.math.BigDecimal;

public record BankRules(
        BigDecimal savingsMinimumBalance,
        int savingsMonthlyWithdrawalLimit,
        BigDecimal savingsAnnualInterestRate,
        BigDecimal currentOverdraftLimit,
        BigDecimal fixedDepositAnnualInterestRate) {

    public BankRules {
        requireNonNegative(savingsMinimumBalance, "Savings minimum balance");
        requireRate(savingsAnnualInterestRate, "Savings interest rate");
        requireNonNegative(currentOverdraftLimit, "Current overdraft limit");
        requireRate(fixedDepositAnnualInterestRate, "Fixed deposit interest rate");
        if (savingsMonthlyWithdrawalLimit <= 0) {
            throw new IllegalArgumentException("Savings withdrawal limit must be positive");
        }
    }

    public static BankRules defaults() {
        return new BankRules(
                new BigDecimal("1000"),
                3,
                new BigDecimal("0.04"),
                new BigDecimal("10000"),
                new BigDecimal("0.07"));
    }

    private static void requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or more");
        }
    }
    
    /** A yearly rate written as a fraction: 0.04 means 4%. Above 1 is almost certainly a typo for a percentage. */
    private static void requireRate(BigDecimal value, String fieldName) {
        requireNonNegative(value, fieldName);
        if (value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be a fraction between 0 and 1, e.g. 0.04 for 4%");
        }
    }
}