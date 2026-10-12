package com.bank.service;

import com.bank.exception.TransactionNotAllowedException;
import com.bank.model.Account;
import com.bank.model.AccountStatus;
import com.bank.model.Transaction;
import com.bank.model.TransactionSearch;
import com.bank.model.TransactionType;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionService {

    private static final int MINI_STATEMENT_SIZE = 5;

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Ledger ledger;
    private final Clock clock;

    public TransactionService(AccountService accountService,
                              AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              Ledger ledger,
                              Clock clock) {
        this.accountService = Objects.requireNonNull(accountService);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.ledger = Objects.requireNonNull(ledger);
        this.clock = Objects.requireNonNull(clock);
    }

    // ---------- Money movement ----------

    public Transaction deposit(String accountNumber, BigDecimal amount) {
        Account account = accountService.getByNumber(accountNumber);
        String number = account.getAccountNumber();

        attempt(TransactionType.DEPOSIT, null, number, amount,
                () -> account.deposit(amount));

        accountRepository.update(account);
        return ledger.recordSuccess(TransactionType.DEPOSIT, null, number, amount);
    }

    public Transaction withdraw(String accountNumber, BigDecimal amount) {
        Account account = accountService.getByNumber(accountNumber);
        String number = account.getAccountNumber();
        LocalDate today = today();

        attempt(TransactionType.WITHDRAWAL, number, null, amount,
                () -> account.withdraw(amount, today));

        accountRepository.update(account);
        return ledger.recordSuccess(TransactionType.WITHDRAWAL, number, null, amount);
    }

    /** Atomic: either both balances change or neither does. One transaction, one reference. */
    public Transaction transfer(String fromNumber, String toNumber, BigDecimal amount) {
        Account source = accountService.getByNumber(fromNumber);
        Account destination = accountService.getByNumber(toNumber);
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        String from = source.getAccountNumber();
        String to = destination.getAccountNumber();
        LocalDate today = today();

        attempt(TransactionType.TRANSFER, from, to, amount, () -> {
            // Phase 1: check both sides. Nothing has changed yet, so a refusal here is harmless.
            source.checkWithdrawal(amount, today);
            destination.checkDeposit(amount);
            // Phase 2: apply both. The same rules just passed, so neither can be refused now.
            source.withdraw(amount, today);
            destination.deposit(amount);
        });

        accountRepository.update(source);
        accountRepository.update(destination);
        return ledger.recordSuccess(TransactionType.TRANSFER, from, to, amount);
    }

    /** Credits a month's interest to every active account. Returns the interest transactions. */
    public List<Transaction> applyMonthlyInterest() {
        List<Transaction> credited = new ArrayList<>();
        for (Account account : accountRepository.findAll()) {
            if (account.getStatus() != AccountStatus.ACTIVE) {
                continue;
            }
            BigDecimal interest = account.applyMonthlyInterest();
            if (interest.signum() > 0) {
                accountRepository.update(account);
                credited.add(ledger.recordSuccess(TransactionType.INTEREST, null,
                        account.getAccountNumber(), interest));
            }
        }
        return credited;
    }

    // ---------- History ----------

    /** Every transaction of an account, successful and failed, oldest first. */
    public List<Transaction> history(String accountNumber) {
        return search(accountNumber, null, null, null);
    }

    /** Any filter may be null, meaning "any". */
    public List<Transaction> search(String accountNumber, LocalDate from, LocalDate to,
                                   TransactionType type) {
        String number = accountService.getByNumber(accountNumber).getAccountNumber();
        return transactionRepository.search(new TransactionSearch(number, from, to, type));
    }

    /** The last five successful transactions, newest first. */
    public List<Transaction> miniStatement(String accountNumber) {
        return history(accountNumber).stream()
                .filter(Transaction::isSuccessful)
                .sorted(Transaction.CHRONOLOGICAL.reversed())
                .limit(MINI_STATEMENT_SIZE)
                .toList();
    }

    // ---------- Helpers ----------

    /**
     * Runs a balance change. If a banking rule refuses it, records a FAILED transaction and rethrows.
     * Bad input (IllegalArgumentException) is not recorded: it was never a real attempt.
     */
    private void attempt(TransactionType type, String source, String destination,
                         BigDecimal amount, Runnable change) {
        try {
            change.run();
        } catch (TransactionNotAllowedException e) {
            ledger.recordFailure(type, source, destination, amount, e.getMessage());
            throw e;
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}