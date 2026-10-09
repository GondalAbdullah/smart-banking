package com.bank.service;

import com.bank.exception.NotFoundException;
import com.bank.model.Customer;
import com.bank.repository.CustomerRepository;

import java.util.List;
import java.util.Objects;

public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = Objects.requireNonNull(
                customerRepository, "Customer repository is required");
    }

    public Customer register(String fullName, String email, String phone, String address) {
        Customer customer = Customer.register(
                customerRepository.nextId(), fullName, email, phone, address);
        customerRepository.add(customer);
        return customer;
    }

    public Customer updateDetails(String customerId, CustomerUpdate changes) {
        Objects.requireNonNull(changes, "Changes are required");
        Customer current = getById(customerId);

        Customer updated = Customer.restore(
                current.getCustomerId(),
                keepIfBlank(changes.fullName(), current.getFullName()),
                keepIfBlank(changes.email(), current.getEmail()),
                keepIfBlank(changes.phone(), current.getPhone()),
                keepIfBlank(changes.address(), current.getAddress()),
                current.getRegistrationDate(),
                current.getStatus());

        customerRepository.update(updated);
        return updated;
    }

    public Customer getById(String customerId) {
        String id = normalizeId(customerId);
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No customer found with ID: " + id));
    }

    public List<Customer> searchByName(String nameFragment) {
        if (nameFragment == null || nameFragment.isBlank()) {
            throw new IllegalArgumentException("Search text must not be blank");
        }
        return customerRepository.searchByName(nameFragment);
    }

    public List<Customer> listAll() {
        return customerRepository.findAll();
    }

    public void deactivate(String customerId) {
        Customer customer = getById(customerId);
        customer.deactivate();
        customerRepository.update(customer);
    }

    /** Used by the account module: a deactivated customer cannot open new accounts. */
    public Customer getActiveCustomer(String customerId) {
        Customer customer = getById(customerId);
        if (!customer.isActive()) {
            throw new IllegalStateException(
                    "Customer " + customer.getCustomerId() + " is inactive");
        }
        return customer;
    }

    private static String keepIfBlank(String newValue, String currentValue) {
        return (newValue == null || newValue.isBlank()) ? currentValue : newValue;
    }

    private static String normalizeId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID must not be blank");
        }
        return customerId.trim().toUpperCase();
    }
}