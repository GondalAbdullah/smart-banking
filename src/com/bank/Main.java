package com.bank;

import com.bank.event.EventBus;
import com.bank.model.Permission;
import com.bank.notification.ConsoleEmailSender;
import com.bank.notification.NotificationService;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.EmployeeRepository;
import com.bank.repository.LoanRepository;
import com.bank.repository.PropertiesRulesRepository;
import com.bank.repository.RulesRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.csv.CsvAccountRepository;
import com.bank.repository.csv.CsvAuditRepository;
import com.bank.repository.csv.CsvCustomerRepository;
import com.bank.repository.csv.CsvEmployeeRepository;
import com.bank.repository.csv.CsvLoanRepository;
import com.bank.repository.csv.CsvTransactionRepository;
import com.bank.security.PasswordHasher;
import com.bank.service.AccountService;
import com.bank.service.AuditLog;
import com.bank.service.AuthService;
import com.bank.service.CustomerService;
import com.bank.service.EmployeeService;
import com.bank.service.Ledger;
import com.bank.service.LoanService;
import com.bank.service.ReportService;
import com.bank.service.RulesService;
import com.bank.service.Session;
import com.bank.service.TransactionService;
import com.bank.ui.AccountMenu;
import com.bank.ui.AdminMenu;
import com.bank.ui.ConsoleInput;
import com.bank.ui.CustomerMenu;
import com.bank.ui.LoanMenu;
import com.bank.ui.LoginScreen;
import com.bank.ui.MainMenu;
import com.bank.ui.ReportScreen;
import com.bank.ui.TransactionMenu;


import java.nio.file.Path;
import java.time.Clock;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {

    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput(new Scanner(System.in));
        Clock clock = Clock.systemDefaultZone();

        // Repositories: everything is saved in the data folder
        Path data = Path.of("data");
        CustomerRepository customerRepository = new CsvCustomerRepository(data.resolve("customers.csv"));
        AccountRepository accountRepository = new CsvAccountRepository(data.resolve("accounts.csv"));
        TransactionRepository transactionRepository = new CsvTransactionRepository(data.resolve("transactions.csv"));
        EmployeeRepository employeeRepository = new CsvEmployeeRepository(data.resolve("employees.csv"));
        AuditRepository auditRepository = new CsvAuditRepository(data.resolve("audit.csv"));
        LoanRepository loanRepository = new CsvLoanRepository(data.resolve("loans.csv"));
        RulesRepository rulesRepository = new PropertiesRulesRepository(data.resolve("rules.properties"));

        PasswordHasher hasher = new PasswordHasher();
        AuditLog auditLog = new AuditLog(auditRepository, clock);
        Session session = new Session(auditLog);

        EventBus eventBus = new EventBus();
        NotificationService notificationService =
                new NotificationService(customerRepository, new ConsoleEmailSender());
        eventBus.subscribe(notificationService::handle);

        AuthService authService = new AuthService(employeeRepository, hasher, session, auditLog);
        EmployeeService employeeService = new EmployeeService(employeeRepository, hasher, session, auditLog);
        RulesService rulesService = new RulesService(rulesRepository, session);
        Ledger ledger = new Ledger(transactionRepository, session, clock);
        CustomerService customerService = new CustomerService(customerRepository, session);
        AccountService accountService = new AccountService(
                accountRepository,
                customerService,
                rulesService::current,
                loanRepository::hasOutstandingLoan,
                ledger,
                session,
                eventBus::publish,
                clock);
        TransactionService transactionService = new TransactionService(
                accountService, accountRepository, transactionRepository, ledger, session,
                eventBus::publish, clock);
        LoanService loanService = new LoanService(loanRepository, customerService, accountService,
                transactionService, session, eventBus::publish, clock);
        ReportService reportService = new ReportService(
                customerRepository, accountRepository, loanRepository, session, clock);

        Predicate<Permission> allowed = session::can;
        CustomerMenu customerMenu = new CustomerMenu(customerService, accountService, input, allowed);
        AccountMenu accountMenu = new AccountMenu(accountService, input, allowed);
        TransactionMenu transactionMenu = new TransactionMenu(transactionService, accountService, input, allowed);
        LoanMenu loanMenu = new LoanMenu(loanService, input, allowed);
        AdminMenu adminMenu = new AdminMenu(employeeService, rulesService, input, allowed);
        ReportScreen reportScreen = new ReportScreen(reportService);
        MainMenu mainMenu = new MainMenu(authService, customerMenu, accountMenu, transactionMenu,
                loanMenu, adminMenu, reportScreen, input, allowed);
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
