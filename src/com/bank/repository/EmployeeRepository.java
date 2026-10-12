package com.bank.repository;

import com.bank.model.Employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {

    void add(Employee employee);

    void update(Employee employee);

    Optional<Employee> findById(String employeeId);

    List<Employee> findAll();

    /** Returns the next free employee ID, e.g. E0001, E0002... */
    String nextId();
}