package com.bank.testsupport;

import com.bank.event.EventBus;
import com.bank.model.Role;
import com.bank.notification.EmailMessage;
import com.bank.notification.NotificationService;
import com.bank.repository.*;
import com.bank.repository.csv.*;
import com.bank.security.PasswordHasher;
import com.bank.service.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The whole bank wired together in memory, the same way Main does it, with three
 * employees ready to use. Each test creates its own TestBank, so tests never share state.
 */
public final class TestBank {

    public static final String ADMIN = "E0001";
    public static final String TELLER = "E0002";
    public static final String MANAGER = "E0003";

    public final MutableClock clock = new MutableClock(LocalDateTime.of(2026, 10, 15, 10, 0));
    public final List<EmailMessage> outbox = new ArrayList<>();   // emails land here instead of the console
    public final LoanRepository loanRepository;

    public final AuthService auth;
    public final EmployeeService employees;
    public final RulesService rules;
    public final CustomerService customers;
    public final AccountService accounts;
    public final TransactionService transactions;
    public final LoanService loans;
    public final ReportService reports;

    public TestBank() {
        this(new InMemoryRulesRepository());
    }

    public TestBank(RulesRepository rulesRepository) {
        this(new InMemoryCustomerRepository(), new InMemoryAccountRepository(),
                new InMemoryTransactionRepository(), new InMemoryEmployeeRepository(),
                new InMemoryAuditRepository(), new InMemoryLoanRepository(), rulesRepository);
    }

    /**
     * The whole bank saved as CSV files in the given folder, exactly like Main.
     * Creating a second TestBank on the same folder is a restart: new objects, same files.
     */
    public static TestBank onDisk(Path folder) {
        return new TestBank(
                new CsvCustomerRepository(folder.resolve("customers.csv")),
                new CsvAccountRepository(folder.resolve("accounts.csv")),
                new CsvTransactionRepository(folder.resolve("transactions.csv")),
                new CsvEmployeeRepository(folder.resolve("employees.csv")),
                new CsvAuditRepository(folder.resolve("audit.csv")),
                new CsvLoanRepository(folder.resolve("loans.csv")),
                new PropertiesRulesRepository(folder.resolve("rules.properties")));
    }

    private TestBank(CustomerRepository customerRepository, AccountRepository accountRepository,
                     TransactionRepository transactionRepository, EmployeeRepository employeeRepository,
                     AuditRepository auditRepository, LoanRepository loanRepository,
                     RulesRepository rulesRepository) {
        this.loanRepository = loanRepository;
        PasswordHasher hasher = new PasswordHasher(1_000);   // fast hashing for tests
        AuditLog auditLog = new AuditLog(auditRepository, clock);
        Session session = new Session(auditLog);
        EventBus eventBus = new EventBus();
        eventBus.subscribe(new NotificationService(customerRepository, outbox::add)::handle);

        auth = new AuthService(employeeRepository, hasher, session, auditLog);
        employees = new EmployeeService(employeeRepository, hasher, session, auditLog);
        rules = new RulesService(rulesRepository, session);
        Ledger ledger = new Ledger(transactionRepository, session, clock);
        customers = new CustomerService(customerRepository, session);
        accounts = new AccountService(accountRepository, customers, rules::current,
                loanRepository::hasOutstandingLoan, ledger, session, eventBus::publish, clock);
        transactions = new TransactionService(accounts, accountRepository, transactionRepository,
                ledger, session, eventBus::publish, clock);
        loans = new LoanService(loanRepository, customers, accounts, transactions,
                session, eventBus::publish, clock);
        reports = new ReportService(customerRepository, accountRepository, loanRepository, session, clock);

        createStaff();
    }

    /** E0001 admin, E0002 teller, E0003 manager - all with the password from password(). First run only. */
    private void createStaff() {
        if (auth.ensureDefaultAdmin().isEmpty()) {
            return;   // a restart: the staff were saved last time
        }
        auth.login(ADMIN, AuthService.DEFAULT_ADMIN_PASSWORD.toCharArray());
        auth.completeRequiredPasswordChange(password());
        employees.hire("Tina Teller", Role.TELLER, "Temporary1".toCharArray());
        employees.hire("Mo Manager", Role.MANAGER, "Temporary1".toCharArray());
        auth.logout();
        for (String id : List.of(TELLER, MANAGER)) {
            auth.login(id, "Temporary1".toCharArray());
            auth.completeRequiredPasswordChange(password());
            auth.logout();
        }
    }

    // ---------- Switching user ----------

    public TestBank asAdmin() {
        return loginAs(ADMIN);
    }

    public TestBank asTeller() {
        return loginAs(TELLER);
    }

    public TestBank asManager() {
        return loginAs(MANAGER);
    }

    private TestBank loginAs(String employeeId) {
        auth.logout();
        auth.login(employeeId, password());
        return this;
    }

    // ---------- Shortcuts ----------

    /** Registers a customer (the caller must be logged in as someone allowed to) and returns the ID. */
    public String customer(String fullName) {
        String email = fullName.toLowerCase().replace(' ', '.') + "@example.com";
        return customers.register(fullName, email, "03001234567", "Lahore").getCustomerId();
    }

    public static char[] password() {
        return "Password1!".toCharArray();
    }

    /** Money with two decimal places, so it compares equal to account balances. */
    public static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2);
    }
}
