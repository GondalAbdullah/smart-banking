package com.bank.ui;

import com.bank.model.Account;
import com.bank.model.Permission;
import com.bank.service.AccountService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Predicate;

public class AccountMenu {

    private final AccountService accountService;
    private final ConsoleInput input;
    private final Menu menu;

    public AccountMenu(AccountService accountService, ConsoleInput input,
                       Predicate<Permission> allowed) {
        this.accountService = accountService;
        this.input = input;
        this.menu = new Menu("Account Management", input, allowed)
                .option("Open savings account", Permission.OPEN_ACCOUNT, this::openSavings)
                .option("Open current account", Permission.OPEN_ACCOUNT, this::openCurrent)
                .option("Open fixed deposit", Permission.OPEN_ACCOUNT, this::openFixedDeposit)
                .option("View account details and balance", this::viewAccount)
                .option("Freeze account", Permission.MANAGE_ACCOUNT_STATUS, this::freezeAccount)
                .option("Unfreeze account", Permission.MANAGE_ACCOUNT_STATUS, this::unfreezeAccount)
                .option("Close account", Permission.MANAGE_ACCOUNT_STATUS, this::closeAccount);
    }

    public void show() {
        menu.show();
    }

    private void openSavings() {
        String customerId = input.readRequired("Customer ID: ");
        BigDecimal deposit = input.readAmount("Opening deposit: ");
        Account account = accountService.openSavings(customerId, deposit);
        System.out.println("Savings account opened: " + account.getAccountNumber());
    }

    private void openCurrent() {
        String customerId = input.readRequired("Customer ID: ");
        BigDecimal deposit = input.readAmount("Opening deposit: ");
        Account account = accountService.openCurrent(customerId, deposit);
        System.out.println("Current account opened: " + account.getAccountNumber());
    }

    private void openFixedDeposit() {
        String customerId = input.readRequired("Customer ID: ");
        BigDecimal deposit = input.readAmount("Deposit amount: ");
        int termMonths = input.readPositiveInt("Term in months: ");
        Account account = accountService.openFixedDeposit(customerId, deposit, termMonths);
        System.out.println("Fixed deposit opened: " + account.getAccountNumber());
    }

    private void viewAccount() {
        Account account = accountService.getByNumber(input.readRequired("Account number: "));
        AccountPrinter.printDetails(account, LocalDate.now());
    }

    private void freezeAccount() {
        Account account = accountService.getByNumber(input.readRequired("Account number: "));
        AccountPrinter.printDetails(account, LocalDate.now());
        if (input.confirm("Freeze this account?")) {
            accountService.freeze(account.getAccountNumber());
            System.out.println("Account frozen.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private void unfreezeAccount() {
        Account account = accountService.getByNumber(input.readRequired("Account number: "));
        accountService.activate(account.getAccountNumber());
        System.out.println("Account " + account.getAccountNumber() + " is active again.");
    }

    private void closeAccount() {
        Account account = accountService.getByNumber(input.readRequired("Account number: "));
        AccountPrinter.printDetails(account, LocalDate.now());
        if (input.confirm("Close this account? This cannot be undone.")) {
            accountService.close(account.getAccountNumber());
            System.out.println("Account closed.");
        } else {
            System.out.println("Cancelled.");
        }
    }
}