package com.bank.service;

import com.bank.event.BankEvent;
import com.bank.exception.NotFoundException;
import com.bank.model.Account;
import com.bank.model.AuditAction;
import com.bank.model.Customer;
import com.bank.model.Loan;
import com.bank.model.LoanStatus;
import com.bank.model.Permission;
import com.bank.model.TransactionType;
import com.bank.repository.LoanRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class LoanService {

    private final LoanRepository loans;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final Session session;
    private final Consumer<BankEvent> events;
    private final Clock clock;

    public LoanService(LoanRepository loans,
                       CustomerService customerService,
                       AccountService accountService,
                       TransactionService transactionService,
                       Session session,
                       Consumer<BankEvent> events,
                       Clock clock) {
        this.loans = Objects.requireNonNull(loans);
        this.customerService = Objects.requireNonNull(customerService);
        this.accountService = Objects.requireNonNull(accountService);
        this.transactionService = Objects.requireNonNull(transactionService);
        this.session = Objects.requireNonNull(session);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    // ---------- Applying ----------

    public Loan apply(String customerId, String accountNumber, BigDecimal principal) {
        session.require(Permission.RECORD_LOANS);
        Customer customer = customerService.getActiveCustomer(customerId);   // only active customers
        Account account = accountService.getByNumber(accountNumber);
        if (!account.getCustomerId().equals(customer.getCustomerId())) {
            throw new IllegalArgumentException("Account " + account.getAccountNumber()
                    + " does not belong to customer " + customer.getCustomerId());
        }
        // Could this account receive the money right now? Rejects bad amounts, frozen accounts
        // and fixed deposits up front, instead of failing later at approval.
        account.checkDeposit(principal);

        Loan loan = Loan.apply(loans.nextId(), customer.getCustomerId(), account.getAccountNumber(),
                principal, LocalDate.now(clock));
        loans.add(loan);
        return loan;
    }

    // ---------- Deciding (Manager only) ----------

    /** Credits the principal to the linked account, then marks the loan approved. */
    public Loan approve(String loanId) {
        session.require(Permission.APPROVE_LOANS);
        Loan loan = getById(loanId);
        loan.requirePending();                    // check before any money moves
        Account account = accountService.getByNumber(loan.getAccountNumber());

        transactionService.credit(TransactionType.LOAN_DISBURSEMENT, account, loan.getPrincipal());
        loan.approve();                           // only reached if the money arrived
        loans.update(loan);

        session.audit(AuditAction.LOAN_APPROVED, loan.getLoanId() + ": " + loan.getPrincipal()
                + " to " + loan.getAccountNumber());
        events.accept(new BankEvent.LoanApproved(loan.getLoanId(), loan.getCustomerId(),
                loan.getAccountNumber(), loan.getPrincipal()));
        return loan;
    }

    public Loan reject(String loanId) {
        session.require(Permission.APPROVE_LOANS);
        Loan loan = getById(loanId);
        loan.reject();
        loans.update(loan);

        session.audit(AuditAction.LOAN_REJECTED, loan.getLoanId());
        events.accept(new BankEvent.LoanRejected(loan.getLoanId(), loan.getCustomerId(),
                loan.getPrincipal()));
        return loan;
    }

    // ---------- Repaying ----------

    /** Debits the linked account, then reduces what is owed. At zero the loan becomes REPAID. */
    public Loan repay(String loanId, BigDecimal amount) {
        session.require(Permission.RECORD_LOANS);
        Loan loan = getById(loanId);
        BigDecimal money = loan.checkRepayment(amount);   // check before any money moves
        Account account = accountService.getByNumber(loan.getAccountNumber());

        transactionService.debit(TransactionType.LOAN_REPAYMENT, account, money);
        loan.repay(money);
        loans.update(loan);
        return loan;
    }

    // ---------- Queries ----------

    public Loan getById(String loanId) {
        if (loanId == null || loanId.isBlank()) {
            throw new IllegalArgumentException("Loan ID must not be blank");
        }
        String id = loanId.trim().toUpperCase();
        return loans.findById(id)
                .orElseThrow(() -> new NotFoundException("No loan found with ID: " + id));
    }

    public List<Loan> findByCustomer(String customerId) {
        String ownerId = customerService.getById(customerId).getCustomerId();
        return loans.findByCustomerId(ownerId);
    }

    /** Applications waiting for a manager's decision, oldest first. */
    public List<Loan> pending() {
        return loans.findAll().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.PENDING)
                .toList();
    }
}