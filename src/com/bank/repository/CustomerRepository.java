package com.bank.repository;

import com.bank.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    /** Stores a new customer. Fails if the ID already exists. */
    void add(Customer customer);

    /** Persists changes to an existing customer. Fails if the ID is unknown. */
    void update(Customer customer);

    Optional<Customer> findById(String customerId);

    List<Customer> findAll();

    List<Customer> searchByName(String nameFragment);

    boolean existsById(String customerId);

    /** Returns the next free customer ID, e.g. C0001, C0002... */
    String nextId();
}