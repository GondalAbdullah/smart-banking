package com.bank.repository.csv;

import com.bank.model.Account;
import com.bank.model.AccountStatus;
import com.bank.model.CurrentAccount;
import com.bank.model.FixedDepositAccount;
import com.bank.model.SavingsAccount;
import com.bank.repository.AccountRepository;
import com.bank.repository.InMemoryAccountRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * All three account types share one file. The "type" column says which kind each row is;
 * columns that do not apply to that type are left empty.
 */
public class CsvAccountRepository implements AccountRepository {

    private static final String SAVINGS = "SAVINGS";
    private static final String CURRENT = "CURRENT";
    private static final String FIXED_DEPOSIT = "FIXED_DEPOSIT";

    private final CsvFile file;
    private final InMemoryAccountRepository memory = new InMemoryAccountRepository();

    public CsvAccountRepository(Path path) {
        this.file = new CsvFile(path,
                // shared by every account
                "accountNumber", "type", "customerId", "openedDate", "balance", "status",
                // savings and fixed deposit
                "annualInterestRate",
                // savings only
                "minimumBalance", "monthlyWithdrawalLimit", "withdrawalMonth", "withdrawalsThisMonth",
                // current only
                "overdraftLimit",
                // fixed deposit only
                "maturityDate");
        file.readAll(CsvAccountRepository::fromRow).forEach(memory::add);
    }

    @Override
    public void add(Account account) {
        memory.add(account);
        save();
    }

    @Override
    public void update(Account account) {
        memory.update(account);
        save();
    }

    private void save() {
        file.writeAll(memory.findAll(), CsvAccountRepository::toRow);
    }

    @Override
    public Optional<Account> findByNumber(String accountNumber) {
        return memory.findByNumber(accountNumber);
    }

    @Override
    public List<Account> findByCustomerId(String customerId) {
        return memory.findByCustomerId(customerId);
    }

    @Override
    public List<Account> findAll() {
        return memory.findAll();
    }

    @Override
    public String nextAccountNumber() {
        return memory.nextAccountNumber();
    }

    // ---------- Mapping ----------

    private static Map<String, Object> toRow(Account account) {
        Map<String, Object> row = new HashMap<>();
        row.put("accountNumber", account.getAccountNumber());
        row.put("customerId", account.getCustomerId());
        row.put("openedDate", account.getOpenedDate());
        row.put("balance", account.getBalance());
        row.put("status", account.getStatus());

        // Account is sealed, so this switch must cover every type. Add a fourth account type
        // and this stops compiling until you decide how to save it.
        switch (account) {
            case SavingsAccount s -> {
                row.put("type", SAVINGS);
                row.put("annualInterestRate", s.getAnnualInterestRate());
                row.put("minimumBalance", s.getMinimumBalance());
                row.put("monthlyWithdrawalLimit", s.getMonthlyWithdrawalLimit());
                row.put("withdrawalMonth", s.getWithdrawalMonth());
                row.put("withdrawalsThisMonth", s.getWithdrawalsThisMonth());
            }
            case CurrentAccount c -> {
                row.put("type", CURRENT);
                row.put("overdraftLimit", c.getOverdraftLimit());
            }
            case FixedDepositAccount f -> {
                row.put("type", FIXED_DEPOSIT);
                row.put("annualInterestRate", f.getAnnualInterestRate());
                row.put("maturityDate", f.getMaturityDate());
            }
        }
        return row;
    }

    private static Account fromRow(CsvRow row) {
        String number = row.text("accountNumber");
        String customerId = row.text("customerId");
        var openedDate = row.date("openedDate");
        var balance = row.decimal("balance");
        var status = row.enumValue("status", AccountStatus.class);

        return switch (row.text("type")) {
            case SAVINGS -> new SavingsAccount(number, customerId, openedDate, balance, status,
                    row.decimal("minimumBalance"),
                    row.decimal("annualInterestRate"),
                    row.integer("monthlyWithdrawalLimit"),
                    row.yearMonth("withdrawalMonth"),
                    row.integer("withdrawalsThisMonth"));
            case CURRENT -> new CurrentAccount(number, customerId, openedDate, balance, status,
                    row.decimal("overdraftLimit"));
            case FIXED_DEPOSIT -> new FixedDepositAccount(number, customerId, openedDate, balance, status,
                    row.decimal("annualInterestRate"),
                    row.date("maturityDate"));
            default -> throw row.error("unknown account type '" + row.text("type") + "'");
        };
    }
}
