package com.bank.repository;

import com.bank.model.Employee;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryEmployeeRepository implements EmployeeRepository {

    private static final String ID_PREFIX = "E";

    private final Map<String, Employee> employees = new LinkedHashMap<>();

    @Override
    public void add(Employee employee) {
        Objects.requireNonNull(employee, "Employee is required");
        if (employees.putIfAbsent(employee.getEmployeeId(), employee) != null) {
            throw new IllegalArgumentException(
                    "Employee ID already exists: " + employee.getEmployeeId());
        }
    }

    @Override
    public void update(Employee employee) {
        Objects.requireNonNull(employee, "Employee is required");
        if (!employees.containsKey(employee.getEmployeeId())) {
            throw new IllegalArgumentException("No employee with ID: " + employee.getEmployeeId());
        }
        employees.put(employee.getEmployeeId(), employee);
    }

    @Override
    public Optional<Employee> findById(String employeeId) {
        return Optional.ofNullable(employees.get(employeeId));
    }

    @Override
    public List<Employee> findAll() {
        return List.copyOf(employees.values());
    }

    @Override
    public String nextId() {
        int highest = employees.keySet().stream()
                .mapToInt(id -> Integer.parseInt(id.substring(ID_PREFIX.length())))
                .max()
                .orElse(0);
        return "%s%04d".formatted(ID_PREFIX, highest + 1);
    }
}