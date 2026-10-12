package com.bank;

import com.bank.model.Permission;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.EmployeeRepository;
import com.bank.repository.InMemoryAccountRepository;
import com.bank.repository.InMemoryAuditRepository;
import com.bank.repository.InMemoryCustomerRepository;
import com.bank.repository.InMemoryEmployeeRepository;
import com.bank.repository.InMemoryTransactionRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.PropertiesRulesRepository;
import com.bank.repository.RulesRepository;
import com.bank.security.PasswordHasher;
import com.bank.service.ReportService;
import com.bank.service.RulesService;
import com.bank.service.AccountService;
import com.bank.service.AuditLog;
import com.bank.service.AuthService;
import com.bank.service.CustomerService;
import com.bank.service.EmployeeService;
import com.bank.service.Ledger;
import com.bank.service.Session;
import com.bank.service.TransactionService;
import com.bank.ui.AccountMenu;
import com.bank.ui.AdminMenu;
import com.bank.ui.ConsoleInput;
import com.bank.ui.CustomerMenu;
import com.bank.ui.LoginScreen;
import com.bank.ui.MainMenu;
import com.bank.ui.TransactionMenu;
import com.bank.ui.ReportScreen;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {

    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput(new Scanner(System.in));
        Clock clock = Clock.systemDefaultZone();

        // Repositories
        CustomerRepository customerRepository = new InMemoryCustomerRepository();
        AccountRepository accountRepository = new InMemoryAccountRepository();
        TransactionRepository transactionRepository = new InMemoryTransactionRepository();
        EmployeeRepository employeeRepository = new InMemoryEmployeeRepository();
        AuditRepository auditRepository = new InMemoryAuditRepository();
        RulesRepository rulesRepository = new PropertiesRulesRepository(Path.of("data", "rules.properties"));
        // Security
        PasswordHasher hasher = new PasswordHasher();
        AuditLog auditLog = new AuditLog(auditRepository, clock);
        Session session = new Session(auditLog);

        // Services
        AuthService authService = new AuthService(employeeRepository, hasher, session, auditLog);
        EmployeeService employeeService = new EmployeeService(employeeRepository, hasher, session, auditLog);
        RulesService rulesService = new RulesService(rulesRepository, session);
        Ledger ledger = new Ledger(transactionRepository, session, clock);
        CustomerService customerService = new CustomerService(customerRepository, session);
        AccountService accountService = new AccountService(
                accountRepository,
                customerService,
                rulesService::current,  // asks for the rules in force each time an account opens
                accountNumber -> false,
                ledger,
                session,
                clock);
        TransactionService transactionService = new TransactionService(
                accountService, accountRepository, transactionRepository, ledger, session, clock);
        ReportService reportService = new ReportService(customerRepository, accountRepository, session, clock);
        // Menus: they get a read-only question ("may the current user do X?"), never the Session
        Predicate<Permission> allowed = session::can;
        CustomerMenu customerMenu = new CustomerMenu(customerService, accountService, input, allowed);
        AccountMenu accountMenu = new AccountMenu(accountService, input, allowed);
        TransactionMenu transactionMenu = new TransactionMenu(transactionService, accountService, input, allowed);
        AdminMenu adminMenu = new AdminMenu(employeeService, rulesService, input, allowed);
        ReportScreen reportScreen = new ReportScreen(reportService);
        MainMenu mainMenu = new MainMenu(authService, customerMenu, accountMenu, transactionMenu,
                adminMenu, reportScreen, input, allowed);
        LoginScreen loginScreen = new LoginScreen(authService, input);

        authService.ensureDefaultAdmin().ifPresent(admin -> System.out.println(
                "First run: created administrator " + admin.getEmployeeId()
                        + " with temporary password '" + AuthService.DEFAULT_ADMIN_PASSWORD
                        + "'. You will be asked to change it at first login."));

        while (loginScreen.show()) {
            mainMenu.show();
        }
        System.out.println("Goodbye.");
    }
}