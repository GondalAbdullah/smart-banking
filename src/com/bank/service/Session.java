package com.bank.service;

import com.bank.exception.AccessDeniedException;
import com.bank.exception.AuthenticationException;
import com.bank.model.AuditAction;
import com.bank.model.Employee;
import com.bank.model.Permission;

import java.util.Objects;

/**
 * Who is logged in right now, and what they may do.
 * Only AuthService (same package) can start or end a session.
 */
public class Session {

    private final AuditLog auditLog;
    private Employee current;

    public Session(AuditLog auditLog) {
        this.auditLog = Objects.requireNonNull(auditLog);
    }

    void start(Employee employee) {
        this.current = Objects.requireNonNull(employee);
    }

    void end() {
        this.current = null;
    }

    boolean isActive() {
        return current != null;
    }

    Employee employee() {
        if (current == null) {
            throw new AuthenticationException("Nobody is logged in");
        }
        return current;
    }

    /** Safe to hand to the UI as session::can - it answers a question and changes nothing. */
    public boolean can(Permission permission) {
        return current != null && current.can(permission);
    }

    /** The gate every protected service method passes through. Refusals are audited. */
    void require(Permission permission) {
        Employee employee = employee();
        if (employee.isPasswordChangeRequired()) {
            throw new AccessDeniedException("Change your temporary password before doing anything else");
        }
        if (!employee.can(permission)) {
            auditLog.record(employee.getEmployeeId(), AuditAction.ACCESS_DENIED, permission.name());
            throw new AccessDeniedException("A " + employee.getRole().label()
                    + " cannot " + permission.description());
        }
    }

    /** Records a sensitive action on behalf of whoever is logged in. */
    void audit(AuditAction action, String details) {
        auditLog.record(employee().getEmployeeId(), action, details);
    }
}