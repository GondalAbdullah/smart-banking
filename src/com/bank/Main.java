package com.bank;

import com.bank.config.BankRules;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.InMemoryAccountRepository;
import com.bank.repository.InMemoryCustomerRepository;
import com.bank.repository.InMemoryTransactionRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.AccountService;
import com.bank.service.CustomerService;
import com.bank.service.Ledger;
import com.bank.service.TransactionService;
import com.bank.ui.AccountMenu;
import com.bank.ui.ConsoleInput;
import com.bank.ui.CustomerMenu;
import com.bank.ui.TransactionMenu;

import java.time.Clock;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput(new Scanner(System.in));
        Clock clock = Clock.systemDefaultZone();

        // Repositories
        CustomerRepository customerRepository = new InMemoryCustomerRepository();
        AccountRepository accountRepository = new InMemoryAccountRepository();
        TransactionRepository transactionRepository = new InMemoryTransactionRepository();

        // Services
        Ledger ledger = new Ledger(transactionRepository, clock);
        CustomerService customerService = new CustomerService(customerRepository);
        AccountService accountService = new AccountService(
                accountRepository,
                customerService,
                BankRules::defaults,
                accountNumber -> false, // TODO(loans): replace with loanService::hasOutstandingLoan
                ledger,
                clock);
        TransactionService transactionService = new TransactionService(
                accountService, accountRepository, transactionRepository, ledger, clock);

        // Menus
        CustomerMenu customerMenu = new CustomerMenu(customerService, accountService, input);
        AccountMenu accountMenu = new AccountMenu(accountService, input);
        TransactionMenu transactionMenu = new TransactionMenu(transactionService, accountService, input);

        while (true) {
            System.out.println("\n=== Smart Banking System ===");
            System.out.println("1. Customers");
            System.out.println("2. Accounts");
            System.out.println("3. Transactions");
            System.out.println("0. Exit");

            switch (input.readLine("Choose: ")) {
                case "1" -> customerMenu.show();
                case "2" -> accountMenu.show();
                case "3" -> transactionMenu.show();
                case "0" -> {
                    System.out.println("Goodbye.");
                    return;
                }
                default -> System.out.println("Invalid option, try again.");
            }
        }
    }
}