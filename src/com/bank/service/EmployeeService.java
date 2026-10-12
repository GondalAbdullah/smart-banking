package com.bank.service;

import com.bank.exception.NotFoundException;
import com.bank.model.AuditAction;
import com.bank.model.AuditEntry;
import com.bank.model.Employee;
import com.bank.model.Permission;
import com.bank.model.Role;
import com.bank.repository.EmployeeRepository;
import com.bank.security.PasswordHasher;

import java.util.List;
import java.util.Objects;

public class EmployeeService {

    private final EmployeeRepository employees;
    private final PasswordHasher hasher;
    private final Session session;
    private final AuditLog auditLog;

    public EmployeeService(EmployeeRepository employees, PasswordHasher hasher,
                           Session session, AuditLog auditLog) {
        this.employees = Objects.requireNonNull(employees);
        this.hasher = Objects.requireNonNull(hasher);
        this.session = Objects.requireNonNull(session);
        this.auditLog = Objects.requireNonNull(auditLog);
    }

    public Employee hire(String fullName, Role role, char[] temporaryPassword) {
        session.require(Permission.MANAGE_EMPLOYEES);
        AuthService.requireStrongEnough(temporaryPassword);
        Employee employee = Employee.hire(employees.nextId(), fullName, role,
                hasher.hash(temporaryPassword));
        employees.add(employee);
        session.audit(AuditAction.EMPLOYEE_CREATED, employee.getEmployeeId() + " as " + role);
        return employee;
    }

    public List<Employee> listAll() {
        session.require(Permission.MANAGE_EMPLOYEES);
        return employees.findAll();
    }

    public Employee getById(String employeeId) {
        session.require(Permission.MANAGE_EMPLOYEES);
        String id = normalizeId(employeeId);
        return employees.findById(id)
                .orElseThrow(() -> new NotFoundException("No employee found with ID: " + id));
    }

    public void changeRole(String employeeId, Role newRole) {
        Employee employee = getById(employeeId);
        requireNotSelf(employee, "change your own role");
        Role oldRole = employee.getRole();
        employee.changeRole(newRole);
        employees.update(employee);
        session.audit(AuditAction.EMPLOYEE_ROLE_CHANGED,
                employee.getEmployeeId() + ": " + oldRole + " -> " + newRole);
    }

    public void resetPassword(String employeeId, char[] temporaryPassword) {
        Employee employee = getById(employeeId);
        requireNotSelf(employee, "reset your own password here; use 'Change my password'");
        AuthService.requireStrongEnough(temporaryPassword);
        employee.resetPassword(hasher.hash(temporaryPassword));
        employees.update(employee);
        session.audit(AuditAction.EMPLOYEE_PASSWORD_RESET, employee.getEmployeeId());
    }

    public void deactivate(String employeeId) {
        Employee employee = getById(employeeId);
        requireNotSelf(employee, "deactivate yourself");
        employee.deactivate();
        employees.update(employee);
        session.audit(AuditAction.EMPLOYEE_DEACTIVATED, employee.getEmployeeId());
    }

    /** "What employees did": the audit trail, newest first. */
    public List<AuditEntry> auditTrail(int limit) {
        session.require(Permission.VIEW_AUDIT_LOG);
        return auditLog.recent(limit);
    }

    /** An admin can never demote or remove themselves, so at least one admin always remains. */
    private void requireNotSelf(Employee target, String action) {
        if (target.equals(session.employee())) {
            throw new IllegalArgumentException("You cannot " + action);
        }
    }

    private static String normalizeId(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID must not be blank");
        }
        return employeeId.trim().toUpperCase();
    }
}