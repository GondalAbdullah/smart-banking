package com.bank.ui;

import com.bank.model.Loan;
import com.bank.model.LoanStatus;
import com.bank.model.Permission;
import com.bank.service.LoanService;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

public class LoanMenu {

    private static final String ROW_FORMAT = "%-6s %-8s %-8s %14s %14s %-9s %s%n";

    private final LoanService loanService;
    private final ConsoleInput input;
    private final Menu menu;

    public LoanMenu(LoanService loanService, ConsoleInput input, Predicate<Permission> allowed) {
        this.loanService = loanService;
        this.input = input;
        this.menu = new Menu("Loans", input, allowed)
                .option("Record loan application", Permission.RECORD_LOANS, this::apply)
                .option("View pending applications", Permission.APPROVE_LOANS, this::pending)
                .option("Approve loan", Permission.APPROVE_LOANS, this::approve)
                .option("Reject loan", Permission.APPROVE_LOANS, this::reject)
                .option("Record repayment", Permission.RECORD_LOANS, this::repay)
                .option("View customer's loans", this::customerLoans);
    }

    public void show() {
        menu.show();
    }

    private void apply() {
        String customerId = input.readRequired("Customer ID: ");
        String accountNumber = input.readRequired("Account to receive the loan: ");
        BigDecimal principal = input.readAmount("Loan amount: ");
        Loan loan = loanService.apply(customerId, accountNumber, principal);
        System.out.println("Application " + loan.getLoanId() + " recorded for " + loan.getPrincipal()
                + ". A manager must approve it.");
    }

    private void pending() {
        printTable(loanService.pending());
    }

    private void approve() {
        Loan loan = loanService.getById(input.readRequired("Loan ID: "));
        printTable(List.of(loan));
        if (!input.confirm("Approve and credit " + loan.getPrincipal()
                + " to account " + loan.getAccountNumber() + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        loanService.approve(loan.getLoanId());
        System.out.println("Approved. " + loan.getPrincipal() + " credited to " + loan.getAccountNumber() + ".");
    }

    private void reject() {
        Loan loan = loanService.getById(input.readRequired("Loan ID: "));
        printTable(List.of(loan));
        if (!input.confirm("Reject this application?")) {
            System.out.println("Cancelled.");
            return;
        }
        loanService.reject(loan.getLoanId());
        System.out.println("Application " + loan.getLoanId() + " rejected.");
    }

    private void repay() {
        Loan loan = loanService.getById(input.readRequired("Loan ID: "));
        System.out.println("Outstanding: " + loan.getOutstanding()
                + " (taken from account " + loan.getAccountNumber() + ")");
        BigDecimal amount = input.readAmount("Repayment amount: ");
        Loan updated = loanService.repay(loan.getLoanId(), amount);
        System.out.println(updated.getStatus() == LoanStatus.REPAID
                ? "Repayment recorded. The loan is fully repaid."
                : "Repayment recorded. Still outstanding: " + updated.getOutstanding());
    }

    private void customerLoans() {
        printTable(loanService.findByCustomer(input.readRequired("Customer ID: ")));
    }

    private static void printTable(List<Loan> loans) {
        if (loans.isEmpty()) {
            System.out.println("No loans found.");
            return;
        }
        System.out.printf(ROW_FORMAT, "Loan", "Customer", "Account", "Principal", "Outstanding", "Status", "Applied");
        loans.forEach(loan -> System.out.printf(ROW_FORMAT,
                loan.getLoanId(), loan.getCustomerId(), loan.getAccountNumber(),
                loan.getPrincipal(), loan.getOutstanding(), loan.getStatus(), loan.getAppliedOn()));
    }
}