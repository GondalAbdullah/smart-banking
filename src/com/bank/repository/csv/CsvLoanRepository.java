package com.bank.repository.csv;

import com.bank.model.Loan;
import com.bank.model.LoanStatus;
import com.bank.repository.InMemoryLoanRepository;
import com.bank.repository.LoanRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CsvLoanRepository implements LoanRepository {

    private final CsvFile file;
    private final InMemoryLoanRepository memory = new InMemoryLoanRepository();

    public CsvLoanRepository(Path path) {
        this.file = new CsvFile(path,
                "loanId", "customerId", "accountNumber", "principal", "appliedOn", "outstanding", "status");
        file.readAll(CsvLoanRepository::fromRow).forEach(memory::add);
    }

    @Override
    public void add(Loan loan) {
        memory.add(loan);
        save();
    }

    @Override
    public void update(Loan loan) {
        memory.update(loan);
        save();
    }

    private void save() {
        file.writeAll(memory.findAll(), CsvLoanRepository::toRow);
    }

    @Override
    public Optional<Loan> findById(String loanId) {
        return memory.findById(loanId);
    }

    @Override
    public List<Loan> findAll() {
        return memory.findAll();
    }

    @Override
    public List<Loan> findByCustomerId(String customerId) {
        return memory.findByCustomerId(customerId);
    }

    @Override
    public List<Loan> findByAccountNumber(String accountNumber) {
        return memory.findByAccountNumber(accountNumber);
    }

    @Override
    public String nextId() {
        return memory.nextId();
    }

    private static Map<String, Object> toRow(Loan loan) {
        Map<String, Object> row = new HashMap<>();
        row.put("loanId", loan.getLoanId());
        row.put("customerId", loan.getCustomerId());
        row.put("accountNumber", loan.getAccountNumber());
        row.put("principal", loan.getPrincipal());
        row.put("appliedOn", loan.getAppliedOn());
        row.put("outstanding", loan.getOutstanding());
        row.put("status", loan.getStatus());
        return row;
    }

    private static Loan fromRow(CsvRow row) {
        return Loan.restore(
                row.text("loanId"),
                row.text("customerId"),
                row.text("accountNumber"),
                row.decimal("principal"),
                row.date("appliedOn"),
                row.decimal("outstanding"),
                row.enumValue("status", LoanStatus.class));
    }
}
