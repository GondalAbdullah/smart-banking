package com.bank.service;

import com.bank.config.BankRules;
import com.bank.exception.NotFoundException;
import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.FixedDepositAccount;
import com.bank.model.SavingsAccount;
import com.bank.repository.AccountRepository;

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

    public AccountService(AccountRepository accountRepository,
                          CustomerService customerService,
                          Supplier<BankRules> rules,
                          Predicate<String> hasOutstandingLoan) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.customerService = Objects.requireNonNull(customerService);
        this.rules = Objects.requireNonNull(rules);
        this.hasOutstandingLoan = Objects.requireNonNull(hasOutstandingLoan);
    }

    // ---------- Opening ----------

    public SavingsAccount openSavings(String customerId, BigDecimal openingDeposit) {
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        BankRules r = rules.get();
        SavingsAccount account = SavingsAccount.open(
                accountRepository.nextAccountNumber(), ownerId, openingDeposit,
                r.savingsMinimumBalance(), r.savingsAnnualInterestRate(),
                r.savingsMonthlyWithdrawalLimit(), LocalDate.now());
        accountRepository.add(account);
        return account;
    }

    public CurrentAccount openCurrent(String customerId, BigDecimal openingDeposit) {
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        CurrentAccount account = CurrentAccount.open(
                accountRepository.nextAccountNumber(), ownerId, openingDeposit,
                rules.get().currentOverdraftLimit(), LocalDate.now());
        accountRepository.add(account);
        return account;
    }

    public FixedDepositAccount openFixedDeposit(String customerId, BigDecimal deposit,
                                                int termMonths) {
        String ownerId = customerService.getActiveCustomer(customerId).getCustomerId();
        FixedDepositAccount account = FixedDepositAccount.open(
                accountRepository.nextAccountNumber(), ownerId, deposit,
                rules.get().fixedDepositAnnualInterestRate(), termMonths, LocalDate.now());
        accountRepository.add(account);
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