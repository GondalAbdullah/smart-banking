package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A loan linked to one of the customer's accounts.
 * Outstanding is what the customer owes right now: zero until approved, then the principal,
 * reduced by every repayment until it reaches zero and the loan is REPAID.
 */
public final class Loan {

    private final String loanId;
    private final String customerId;
    private final String accountNumber;
    private final BigDecimal principal;
    private final LocalDate appliedOn;
    private BigDecimal outstanding;
    private LoanStatus status;

    private Loan(String loanId, String customerId, String accountNumber, BigDecimal principal,
                 LocalDate appliedOn, BigDecimal outstanding, LoanStatus status) {
        this.loanId = requireNonBlank(loanId, "Loan ID");
        this.customerId = requireNonBlank(customerId, "Customer ID");
        this.accountNumber = requireNonBlank(accountNumber, "Account number");
        this.principal = Account.validAmount(principal);
        this.appliedOn = Objects.requireNonNull(appliedOn, "Application date is required");
        this.outstanding = Account.requireNonNegative(outstanding, "Outstanding amount")
                .setScale(Account.MONEY_SCALE);
        this.status = Objects.requireNonNull(status, "Status is required");
        if (this.outstanding.compareTo(this.principal) > 0) {
            throw new IllegalArgumentException("Outstanding amount cannot exceed the principal");
        }
    }

    /** A new application: pending, nothing owed yet. */
    public static Loan apply(String loanId, String customerId, String accountNumber,
                             BigDecimal principal, LocalDate today) {
        return new Loan(loanId, customerId, accountNumber, principal, today,
                BigDecimal.ZERO, LoanStatus.PENDING);
    }

    /** Rebuilds an existing loan from storage. */
    public static Loan restore(String loanId, String customerId, String accountNumber,
                               BigDecimal principal, LocalDate appliedOn,
                               BigDecimal outstanding, LoanStatus status) {
        return new Loan(loanId, customerId, accountNumber, principal, appliedOn, outstanding, status);
    }

    // ---------- Decisions ----------

    /** Call before moving any money: only a pending loan can be approved or rejected. */
    public void requirePending() {
        if (status != LoanStatus.PENDING) {
            throw new IllegalStateException("Loan " + loanId + " is " + status
                    + "; only a PENDING loan can be approved or rejected");
        }
    }

    public void approve() {
        requirePending();
        status = LoanStatus.APPROVED;
        outstanding = principal;
    }

    public void reject() {
        requirePending();
        status = LoanStatus.REJECTED;
    }

    // ---------- Repayment: check first, then change ----------

    /** Runs every repayment rule without changing anything. Returns the normalised amount. */
    public BigDecimal checkRepayment(BigDecimal amount) {
        BigDecimal money = Account.validAmount(amount);
        if (status != LoanStatus.APPROVED) {
            throw new IllegalStateException("Loan " + loanId + " is " + status
                    + "; only an APPROVED loan can be repaid");
        }
        if (money.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("Repayment exceeds the outstanding amount of " + outstanding);
        }
        return money;
    }

    public void repay(BigDecimal amount) {
        BigDecimal money = checkRepayment(amount);
        outstanding = outstanding.subtract(money);
        if (outstanding.signum() == 0) {
            status = LoanStatus.REPAID;
        }
    }

    /** A pending or approved loan ties up its account, which therefore cannot be closed. */
    public boolean isLiability() {
        return status == LoanStatus.PENDING || status == LoanStatus.APPROVED;
    }

    // ---------- Getters ----------

    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getPrincipal() { return principal; }
    public LocalDate getAppliedOn() { return appliedOn; }
    public BigDecimal getOutstanding() { return outstanding; }
    public LoanStatus getStatus() { return status; }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loan other)) return false;
        return loanId.equals(other.loanId);
    }

    @Override
    public int hashCode() {
        return loanId.hashCode();
    }
}