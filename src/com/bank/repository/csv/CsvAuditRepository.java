package com.bank.repository.csv;

import com.bank.model.AuditAction;
import com.bank.model.AuditEntry;
import com.bank.repository.AuditRepository;
import com.bank.repository.InMemoryAuditRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Append-only, like transactions: an audit trail you can rewrite is not an audit trail. */
public class CsvAuditRepository implements AuditRepository {

    private final CsvFile file;
    private final InMemoryAuditRepository memory = new InMemoryAuditRepository();

    public CsvAuditRepository(Path path) {
        this.file = new CsvFile(path, "timestamp", "employeeId", "action", "details");
        file.readAll(CsvAuditRepository::fromRow).forEach(memory::add);
    }

    @Override
    public void add(AuditEntry entry) {
        memory.add(entry);
        file.append(toRow(entry));
    }

    @Override
    public List<AuditEntry> findAll() {
        return memory.findAll();
    }

    private static Map<String, Object> toRow(AuditEntry entry) {
        Map<String, Object> row = new HashMap<>();
        row.put("timestamp", entry.timestamp());
        row.put("employeeId", entry.employeeId());
        row.put("action", entry.action());
        row.put("details", entry.details());
        return row;
    }

    private static AuditEntry fromRow(CsvRow row) {
        return new AuditEntry(
                row.dateTime("timestamp"),
                row.text("employeeId"),
                row.enumValue("action", AuditAction.class),
                row.text("details"));
    }
}
