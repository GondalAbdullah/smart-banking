package com.bank.repository;

import com.bank.model.Customer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryCustomerRepository implements CustomerRepository {

    private static final String ID_PREFIX = "C";

    private final Map<String, Customer> customers = new LinkedHashMap<>();

    @Override
    public void add(Customer customer) {
        Objects.requireNonNull(customer, "Customer is required");
        Customer existing = customers.putIfAbsent(customer.getCustomerId(), customer);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "Customer ID already exists: " + customer.getCustomerId());
        }
    }

    @Override
    public void update(Customer customer) {
        Objects.requireNonNull(customer, "Customer is required");
        if (!customers.containsKey(customer.getCustomerId())) {
            throw new IllegalArgumentException(
                    "No customer with ID: " + customer.getCustomerId());
        }
        customers.put(customer.getCustomerId(), customer);
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(customers.values());
    }

    @Override
    public List<Customer> searchByName(String nameFragment) {
        Objects.requireNonNull(nameFragment, "Search text is required");
        String needle = nameFragment.trim().toLowerCase();

        return customers.values().stream()
                .filter(c -> c.getFullName().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Customer::getFullName))
                .toList();
    }

    @Override
    public boolean existsById(String customerId) {
        return customers.containsKey(customerId);
    }

    @Override
    public String nextId() {
        int highest = customers.keySet().stream()
                .mapToInt(id -> Integer.parseInt(id.substring(ID_PREFIX.length())))
                .max()
                .orElse(0);
        return "%s%04d".formatted(ID_PREFIX, highest + 1);
    }
}