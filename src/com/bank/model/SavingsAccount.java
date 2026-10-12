package com.bank.model;

import com.bank.exception.TransactionNotAllowedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class SavingsAccount extends Account {

    private final BigDecimal minimumBalance;
    private final BigDecimal annualInterestRate;
    private final int monthlyWithdrawalLimit;
    private YearMonth withdrawalMonth;
    private int withdrawalsThisMonth;

    /** Full constructor, used when loading from storage. */
    public SavingsAccount(String accountNumber, String customerId, LocalDate openedDate,
                          BigDecimal balance, AccountStatus status,
                          BigDecimal minimumBalance, BigDecimal annualInterestRate,
                          int monthlyWithdrawalLimit,
                          YearMonth withdrawalMonth, int withdrawalsThisMonth) {
        super(accountNumber, customerId, openedDate, balance, status);
        this.minimumBalance = requireNonNegative(minimumBalance, "Minimum balance");
        this.annualInterestRate = requireNonNegative(annualInterestRate, "Interest rate");
        if (monthlyWithdrawalLimit <= 0) {
            throw new IllegalArgumentException("Monthly withdrawal limit must be positive");
        }
        if (withdrawalsThisMonth < 0) {
            throw new IllegalArgumentException("Withdrawal count cannot be negative");
        }
        this.monthlyWithdrawalLimit = monthlyWithdrawalLimit;
        this.withdrawalMonth = Objects.requireNonNull(withdrawalMonth, "Withdrawal month is required");
        this.withdrawalsThisMonth = withdrawalsThisMonth;
    }

    /** Opens a brand-new savings account. */
    public static SavingsAccount open(String accountNumber, String customerId,
                                      BigDecimal openingDeposit, BigDecimal minimumBalance,
                                      BigDecimal annualInterestRate, int monthlyWithdrawalLimit,
                                      LocalDate today) {
        BigDecimal deposit = validAmount(openingDeposit);
        if (deposit.compareTo(minimumBalance) < 0) {
            throw new TransactionNotAllowedException(
                    "Opening deposit must be at least the minimum balance of " + minimumBalance);
        }
        return new SavingsAccount(accountNumber, customerId, today, deposit, AccountStatus.ACTIVE,
                minimumBalance, annualInterestRate, monthlyWithdrawalLimit,
                YearMonth.from(today), 0);
    }

    @Override
    protected void validateWithdrawal(BigDecimal amount, LocalDate today) {
        if (withdrawalsUsedIn(YearMonth.from(today)) >= monthlyWithdrawalLimit) {
            throw new TransactionNotAllowedException(
                    "Monthly limit of " + monthlyWithdrawalLimit + " withdrawals reached");
        }
        if (getBalance().subtract(amount).compareTo(minimumBalance) < 0) {
            BigDecimal available = getBalance().subtract(minimumBalance).max(BigDecimal.ZERO);
            throw new TransactionNotAllowedException("Balance cannot go below the minimum of "
                    + minimumBalance + "; you can withdraw at most " + available);
        }
    }

    @Override
    protected void onWithdrawal(LocalDate today) {
        YearMonth month = YearMonth.from(today);
        if (!month.equals(withdrawalMonth)) {
            withdrawalMonth = month;
            withdrawalsThisMonth = 0;
        }
        withdrawalsThisMonth++;
    }

    @Override
    public BigDecimal calculateMonthlyInterest() {
        if (getBalance().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return monthlyInterest(getBalance(), annualInterestRate);
    }

    @Override
    public String typeLabel() {
        return "Savings";
    }

    public int remainingWithdrawals(LocalDate today) {
        return monthlyWithdrawalLimit - withdrawalsUsedIn(YearMonth.from(today));
    }

    private int withdrawalsUsedIn(YearMonth month) {
        return month.equals(withdrawalMonth) ? withdrawalsThisMonth : 0;
    }

    public BigDecimal getMinimumBalance() { return minimumBalance; }
    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public int getMonthlyWithdrawalLimit() { return monthlyWithdrawalLimit; }
    public YearMonth getWithdrawalMonth() { return withdrawalMonth; }
    public int getWithdrawalsThisMonth() { return withdrawalsThisMonth; }
}