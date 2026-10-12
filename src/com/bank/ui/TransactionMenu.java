package com.bank.ui;

import com.bank.model.Account;
import com.bank.model.Permission;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.service.AccountService;
import com.bank.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

public class TransactionMenu {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final ConsoleInput input;
    private final Menu menu;

    public TransactionMenu(TransactionService transactionService, AccountService accountService,
                           ConsoleInput input, Predicate<Permission> allowed) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.input = input;
        this.menu = new Menu("Transactions", input, allowed)
                .option("Deposit", Permission.PROCESS_TRANSACTIONS, this::deposit)
                .option("Withdraw", Permission.PROCESS_TRANSACTIONS, this::withdraw)
                .option("Transfer", Permission.PROCESS_TRANSACTIONS, this::transfer)
                .option("Transaction history", this::history)
                .option("Search transactions", this::search)
                .option("Mini statement (last 5)", this::miniStatement)
                .option("Apply monthly interest", Permission.APPLY_INTEREST, this::applyInterest);
    }

    public void show() {
        menu.show();
    }

    private void deposit() {
        String number = input.readRequired("Account number: ");
        BigDecimal amount = input.readAmount("Amount: ");
        TransactionPrinter.printReceipt(transactionService.deposit(number, amount));
    }

    private void withdraw() {
        String number = input.readRequired("Account number: ");
        BigDecimal amount = input.readAmount("Amount: ");
        TransactionPrinter.printReceipt(transactionService.withdraw(number, amount));
    }

    private void transfer() {
        String from = input.readRequired("From account: ");
        String to = input.readRequired("To account: ");
        BigDecimal amount = input.readAmount("Amount: ");
        if (!input.confirm("Transfer " + amount + " from " + from + " to " + to + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        TransactionPrinter.printReceipt(transactionService.transfer(from, to, amount));
    }

    private void history() {
        String number = readAccount().getAccountNumber();
        TransactionPrinter.printStatement(number, transactionService.history(number));
    }

    private void search() {
        String number = readAccount().getAccountNumber();
        System.out.println("Press Enter to skip any filter.");
        LocalDate from = input.readOptionalDate("From date (YYYY-MM-DD): ");
        LocalDate to = input.readOptionalDate("To date (YYYY-MM-DD): ");
        TransactionType type = input.chooseOptional("Type: ", TransactionType.class);
        TransactionPrinter.printStatement(number, transactionService.search(number, from, to, type));
    }

    private void miniStatement() {
        Account account = readAccount();
        String number = account.getAccountNumber();
        System.out.println("Mini statement for " + number + " | Balance: " + account.getBalance());
        TransactionPrinter.printStatement(number, transactionService.miniStatement(number));
    }

    private void applyInterest() {
        if (!input.confirm("Apply this month's interest to every active account?")) {
            System.out.println("Cancelled.");
            return;
        }
        List<Transaction> credited = transactionService.applyMonthlyInterest();
        BigDecimal total = credited.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Interest credited to " + credited.size() + " account(s), total " + total);
    }

    private Account readAccount() {
        return accountService.getByNumber(input.readRequired("Account number: "));
    }
}