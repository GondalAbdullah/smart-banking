package com.bank.repository.csv;

import com.bank.model.Customer;
import com.bank.model.CustomerStatus;
import com.bank.repository.CustomerRepository;
import com.bank.repository.InMemoryCustomerRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Customers kept in memory while the app runs, and saved to a CSV file after every change.
 * Queries and ID generation are handed to the in-memory repository, so that logic lives in one place.
 */
public class CsvCustomerRepository implements CustomerRepository {

    private final CsvFile file;
    private final InMemoryCustomerRepository memory = new InMemoryCustomerRepository();

    public CsvCustomerRepository(Path path) {
        this.file = new CsvFile(path,
                "customerId", "fullName", "email", "phone", "address", "registrationDate", "status");
        file.readAll(CsvCustomerRepository::fromRow).forEach(memory::add);   // add() also rejects duplicate IDs
    }

    // ---------- Changes: update memory, then save the file ----------

    @Override
    public void add(Customer customer) {
        memory.add(customer);
        save();
    }

    @Override
    public void update(Customer customer) {
        memory.update(customer);
        save();
    }

    private void save() {
        file.writeAll(memory.findAll(), CsvCustomerRepository::toRow);
    }

    // ---------- Queries: straight from memory ----------

    @Override
    public Optional<Customer> findById(String customerId) {
        return memory.findById(customerId);
    }

    @Override
    public List<Customer> findAll() {
        return memory.findAll();
    }

    @Override
    public List<Customer> searchByName(String nameFragment) {
        return memory.searchByName(nameFragment);
    }

    @Override
    public boolean existsById(String customerId) {
        return memory.existsById(customerId);
    }

    @Override
    public String nextId() {
        return memory.nextId();
    }

    // ---------- Mapping ----------

    private static Map<String, Object> toRow(Customer customer) {
        Map<String, Object> row = new HashMap<>();
        row.put("customerId", customer.getCustomerId());
        row.put("fullName", customer.getFullName());
        row.put("email", customer.getEmail());
        row.put("phone", customer.getPhone());
        row.put("address", customer.getAddress());
        row.put("registrationDate", customer.getRegistrationDate());
        row.put("status", customer.getStatus());
        return row;
    }

    /** Goes through Customer.restore, so a hand-edited file still has to pass the model's validation. */
    private static Customer fromRow(CsvRow row) {
        return Customer.restore(
                row.text("customerId"),
                row.text("fullName"),
                row.text("email"),
                row.text("phone"),
                row.text("address"),
                row.date("registrationDate"),
                row.enumValue("status", CustomerStatus.class));
    }
}
