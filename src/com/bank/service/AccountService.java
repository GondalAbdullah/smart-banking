package com.bank.service;

import com.bank.config.BankRules;
import com.bank.exception.NotFoundException;
import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.FixedDepositAccount;
import com.bank.model.SavingsAccount;
import com.bank.repository.AccountRepository;
import com.bank.model.TransactionType;

import java.time.Clock;
import java.math.BigDecimal;
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
    private final Clock clock;

    public AccountService(AccountRepository accountRepository,
                          CustomerService customerService,
                          Supplier<BankRules> rules,
                          Predicate<String> hasOutstandingLoan,
                          Ledger ledger,
                          Clock clock) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.customerService = Objects.requireNonNull(customerService);
        this.rules = Objects.requireNonNull(rules);
        this.hasOutstandingLoan = Objects.requireNonNull(hasOutstandingLoan);
        this.ledger = Objects.requireNonNull(ledger);
        this.clock = Objects.requireNonNull(clock);
    }

    // ---------- Opening ----------

    public SavingsAccount openSavings(String customerId, BigDecimal openingDeposit) {
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        BankRules r = rules.get();
        return saveNew(SavingsAccount.open(
                accountRepository.nextAccountNumber(), ownerId, openingDeposit,
                r.savingsMinimumBalance(), r.savingsAnnualInterestRate(),
                r.savingsMonthlyWithdrawalLimit(), today()));
    }

    public CurrentAccount openCurrent(String customerId, BigDecimal openingDeposit) {
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        return saveNew(CurrentAccount.open(
                accountRepository.nextAccountNumber(), ownerId, openingDeposit,
                rules.get().currentOverdraftLimit(), today()));
    }

    public FixedDepositAccount openFixedDeposit(String customerId, BigDecimal deposit,
                                                int termMonths) {
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

    private LocalDate today() {
        return LocalDate.now(clock);
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
        Account account = getByNumber(accountNumber);
        account.freeze();
        accountRepository.update(account);
    }

    public void activate(String accountNumber) {
        Account account = getByNumber(accountNumber);
        account.activate();
        accountRepository.update(account);
    }

    public void close(String accountNumber) {
        Account account = getByNumber(accountNumber);
        if (hasOutstandingLoan.test(account.getAccountNumber())) {
            throw new IllegalStateException("Account " + account.getAccountNumber()
                    + " has an outstanding loan and cannot be closed");
        }
        account.close();
        accountRepository.update(account);
    }

    private static String normalize(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number must not be blank");
        }
        return accountNumber.trim().toUpperCase();
    }
}