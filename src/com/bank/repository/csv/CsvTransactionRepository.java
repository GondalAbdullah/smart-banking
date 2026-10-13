package com.bank.repository.csv;

import com.bank.model.Transaction;
import com.bank.model.TransactionSearch;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;
import com.bank.repository.InMemoryTransactionRepository;
import com.bank.repository.TransactionRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactions are never edited or deleted, so a new one is appended as one line
 * instead of rewriting the whole file. The file only ever grows.
 */
public class CsvTransactionRepository implements TransactionRepository {

    private final CsvFile file;
    private final InMemoryTransactionRepository memory = new InMemoryTransactionRepository();

    public CsvTransactionRepository(Path path) {
        this.file = new CsvFile(path,
                "transactionId", "type", "sourceAccount", "destinationAccount", "amount",
                "timestamp", "status", "note", "performedBy");
        // Loading through add() also restores the highest ID seen, so nextId() continues
        // from T000042 after a restart instead of starting again at T000001.
        file.readAll(CsvTransactionRepository::fromRow).forEach(memory::add);
    }

    @Override
    public void add(Transaction transaction) {
        memory.add(transaction);
        file.append(toRow(transaction));
    }

    @Override
    public List<Transaction> search(TransactionSearch criteria) {
        return memory.search(criteria);
    }

    @Override
    public String nextId() {
        return memory.nextId();
    }

    // ---------- Mapping ----------

    private static Map<String, Object> toRow(Transaction t) {
        Map<String, Object> row = new HashMap<>();
        row.put("transactionId", t.transactionId());
        row.put("type", t.type());
        row.put("sourceAccount", t.sourceAccount());             // null -> empty field
        row.put("destinationAccount", t.destinationAccount());   // null -> empty field
        row.put("amount", t.amount());
        row.put("timestamp", t.timestamp());
        row.put("status", t.status());
        row.put("note", t.note());                               // only failed ones have a note
        row.put("performedBy", t.performedBy());
        return row;
    }

    private static Transaction fromRow(CsvRow row) {
        return new Transaction(
                row.text("transactionId"),
                row.enumValue("type", TransactionType.class),
                row.optional("sourceAccount"),
                row.optional("destinationAccount"),
                row.decimal("amount"),
                row.dateTime("timestamp"),
                row.enumValue("status", TransactionStatus.class),
                row.optional("note"),
                row.text("performedBy"));
    }
}
