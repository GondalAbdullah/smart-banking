package com.bank.model;

import com.bank.exception.TransactionNotAllowedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

public abstract sealed class Account
        permits SavingsAccount, CurrentAccount, FixedDepositAccount {

    protected static final int MONEY_SCALE = 2;

    private final String accountNumber;
    private final String customerId;
    private final LocalDate openedDate;
    private BigDecimal balance;
    private AccountStatus status;

    protected Account(String accountNumber, String customerId, LocalDate openedDate,
                      BigDecimal balance, AccountStatus status) {
        this.accountNumber = requireNonBlank(accountNumber, "Account number");
        this.customerId = requireNonBlank(customerId, "Customer ID");
        this.openedDate = Objects.requireNonNull(openedDate, "Opened date is required");
        this.balance = Objects.requireNonNull(balance, "Balance is required")
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        this.status = Objects.requireNonNull(status, "Status is required");
    }

    // ---------- Template methods: the fixed steps every account follows ----------

    public final void deposit(BigDecimal amount) {
        requireActive();
        BigDecimal money = validAmount(amount);
        validateDeposit(money);
        balance = balance.add(money);
    }

    public final void withdraw(BigDecimal amount, LocalDate today) {
        requireActive();
        BigDecimal money = validAmount(amount);
        Objects.requireNonNull(today, "Date is required");
        validateWithdrawal(money, today);
        balance = balance.subtract(money);
        onWithdrawal(today);
    }

    /** Adds this month's interest to the balance and returns the amount added. */
    public final BigDecimal applyMonthlyInterest() {
        requireActive();
        BigDecimal interest = calculateMonthlyInterest();
        if (interest.signum() > 0) {
            balance = balance.add(interest);
        }
        return interest;
    }

    // ---------- Hooks: each account type fills these in ----------

    /** Throw TransactionNotAllowedException if this withdrawal breaks a rule. */
    protected abstract void validateWithdrawal(BigDecimal amount, LocalDate today);

    public abstract BigDecimal calculateMonthlyInterest();

    public abstract String typeLabel();

    /** Default: any positive deposit is fine. Override to restrict. */
    protected void validateDeposit(BigDecimal amount) {
    }

    /** Default: nothing to track after a withdrawal. Override if needed. */
    protected void onWithdrawal(LocalDate today) {
    }

    // ---------- Status changes ----------

    public void freeze() {
        requireNotClosed();
        status = AccountStatus.FROZEN;
    }

    public void activate() {
        requireNotClosed();
        status = AccountStatus.ACTIVE;
    }

    public void close() {
        requireNotClosed();
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account " + accountNumber
                    + " can only be closed with a zero balance (current: " + balance + ")");
        }
        status = AccountStatus.CLOSED;
    }

    // ---------- Getters ----------

    public String getAccountNumber() { return accountNumber; }
    public String getCustomerId() { return customerId; }
    public LocalDate getOpenedDate() { return openedDate; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }

    public boolean isClosed() {
        return status == AccountStatus.CLOSED;
    }

    // ---------- Shared helpers for subclasses ----------

    protected final void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new TransactionNotAllowedException(
                    "Account " + accountNumber + " is " + status + "; transactions are not allowed");
        }
    }

    private void requireNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("Account " + accountNumber + " is closed");
        }
    }

    /** Validates a money amount: positive, at most 2 decimal places. */
    protected static BigDecimal validAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > MONEY_SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
        return amount.setScale(MONEY_SCALE);
    }

    protected static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or more");
        }
        return value;
    }

    /** balance × annual rate ÷ 12, rounded to 2 decimals. */
    protected static BigDecimal monthlyInterest(BigDecimal base, BigDecimal annualRate) {
        return base.multiply(annualRate)
                .divide(BigDecimal.valueOf(12), MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    // ---------- Identity ----------

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return accountNumber.equals(other.accountNumber);
    }

    @Override
    public final int hashCode() {
        return accountNumber.hashCode();
    }
}