package com.bank.service;

import com.bank.config.BankRules;
import com.bank.exception.NotFoundException;
import com.bank.model.Account;
import com.bank.model.AuditAction;
import com.bank.model.CurrentAccount;
import com.bank.model.FixedDepositAccount;
import com.bank.model.Permission;
import com.bank.model.SavingsAccount;
import com.bank.model.TransactionType;
import com.bank.repository.AccountRepository;
import com.bank.event.BankEvent;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final Supplier<BankRules> rules;
    private final Predicate<String> hasOutstandingLoan;
    private final Ledger ledger;
    private final Session session;
    private final Consumer<BankEvent> events;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository,
                          CustomerService customerService,
                          Supplier<BankRules> rules,
                          Predicate<String> hasOutstandingLoan,
                          Ledger ledger,
                          Session session,
                          Consumer<BankEvent> events,
                          Clock clock) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.customerService = Objects.requireNonNull(customerService);
        this.rules = Objects.requireNonNull(rules);
        this.hasOutstandingLoan = Objects.requireNonNull(hasOutstandingLoan);
        this.ledger = Objects.requireNonNull(ledger);
        this.session = Objects.requireNonNull(session);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    // ---------- Opening ----------

    public SavingsAccount openSavings(String customerId, BigDecimal openingDeposit) {
        session.require(Permission.OPEN_ACCOUNT);
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        BankRules r = rules.get();
        return saveNew(SavingsAccount.open(
                accountRepository.nextAccountNumber(), ownerId, openingDeposit,
                r.savingsMinimumBalance(), r.savingsAnnualInterestRate(),
                r.savingsMonthlyWithdrawalLimit(), today()));
    }

    public CurrentAccount openCurrent(String customerId, BigDecimal openingDeposit) {
        session.require(Permission.OPEN_ACCOUNT);
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        return saveNew(CurrentAccount.open(
                accountRepository.nextAccountNumber(), ownerId, openingDeposit,
                rules.get().currentOverdraftLimit(), today()));
    }

    public FixedDepositAccount openFixedDeposit(String customerId, BigDecimal deposit,
                                                int termMonths) {
        session.require(Permission.OPEN_ACCOUNT);
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        return saveNew(FixedDepositAccount.open(
                accountRepository.nextAccountNumber(), ownerId, deposit,
                rules.get().fixedDepositAnnualInterestRate(), termMonths, today()));
    }

    /** Stores a newly opened account and records its opening deposit. Returns the same type it was given. */
    private <T extends Account> T saveNew(T account) {
        accountRepository.add(account);
        ledger.recordSuccess(TransactionType.OPENING_DEPOSIT, null,
                account.getAccountNumber(), account.getBalance());
        return account;
    }

    // ---------- Queries ----------

    public Account getByNumber(String accountNumber) {
        String number = normalize(accountNumber);
        return accountRepository.findByNumber(number)
                .orElseThrow(() -> new NotFoundException("No account found with number: " + number));
    }

    public List<Account> findByCustomer(String customerId) {
        String ownerId = customerService.getById(customerId).getCustomerId();
        return accountRepository.findByCustomerId(ownerId);
    }

    // ---------- Status ----------

    public void freeze(String accountNumber) {
        session.require(Permission.MANAGE_ACCOUNT_STATUS);
        Account account = getByNumber(accountNumber);
        account.freeze();
        accountRepository.update(account);
        session.audit(AuditAction.ACCOUNT_FROZEN, account.getAccountNumber());
    }

    public void activate(String accountNumber) {
        session.require(Permission.MANAGE_ACCOUNT_STATUS);
        Account account = getByNumber(accountNumber);
        account.activate();
        accountRepository.update(account);
        session.audit(AuditAction.ACCOUNT_UNFROZEN, account.getAccountNumber());
    }

    public void close(String accountNumber) {
        session.require(Permission.MANAGE_ACCOUNT_STATUS);
        Account account = getByNumber(accountNumber);
        if (hasOutstandingLoan.test(account.getAccountNumber())) {
            throw new IllegalStateException("Account " + account.getAccountNumber()
                    + " has an outstanding loan and cannot be closed");
        }
        account.close();
        accountRepository.update(account);
        session.audit(AuditAction.ACCOUNT_CLOSED, account.getAccountNumber());
        events.accept(new BankEvent.AccountClosed(
                account.getAccountNumber(), account.getCustomerId(), LocalDateTime.now(clock)));    
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String normalize(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number must not be blank");
        }
        return accountNumber.trim().toUpperCase();
    }
}