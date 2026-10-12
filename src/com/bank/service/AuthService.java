package com.bank.service;

import com.bank.exception.AuthenticationException;
import com.bank.model.AuditAction;
import com.bank.model.Employee;
import com.bank.model.Role;
import com.bank.repository.EmployeeRepository;
import com.bank.security.PasswordHasher;

import java.util.Objects;
import java.util.Optional;

public class AuthService {

    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_ADMIN_NAME = "System Administrator";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final String INVALID_CREDENTIALS = "Invalid employee ID or password";
    private static final String ACCOUNT_LOCKED =
            "This account is locked. Ask an administrator to reset your password.";

    private final EmployeeRepository employees;
    private final PasswordHasher hasher;
    private final Session session;
    private final AuditLog auditLog;

    public AuthService(EmployeeRepository employees, PasswordHasher hasher,
                       Session session, AuditLog auditLog) {
        this.employees = Objects.requireNonNull(employees);
        this.hasher = Objects.requireNonNull(hasher);
        this.session = Objects.requireNonNull(session);
        this.auditLog = Objects.requireNonNull(auditLog);
    }

    /** First run only: creates an administrator with a temporary password. */
    public Optional<Employee> ensureDefaultAdmin() {
        if (!employees.findAll().isEmpty()) {
            return Optional.empty();
        }
        Employee admin = Employee.hire(employees.nextId(), DEFAULT_ADMIN_NAME, Role.ADMIN,
                hasher.hash(DEFAULT_ADMIN_PASSWORD.toCharArray()));
        employees.add(admin);
        auditLog.record(AuditLog.SYSTEM, AuditAction.EMPLOYEE_CREATED,
                admin.getEmployeeId() + " as default administrator");
        return Optional.of(admin);
    }

    public Employee login(String employeeId, char[] password) {
        String id = (employeeId == null) ? "" : employeeId.trim().toUpperCase();
        Optional<Employee> found = employees.findById(id);

        if (found.isEmpty()) {
            auditLog.record(id, AuditAction.LOGIN_FAILED, "unknown employee ID");
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        Employee employee = found.get();

        if (!employee.isActive()) {
            auditLog.record(id, AuditAction.LOGIN_FAILED, "account " + employee.getStatus());
            throw new AuthenticationException(employee.isLocked() ? ACCOUNT_LOCKED : INVALID_CREDENTIALS);
        }

        if (!hasher.matches(password, employee.getPasswordHash())) {
            employee.recordFailedLogin();
            employees.update(employee);
            auditLog.record(id, AuditAction.LOGIN_FAILED,
                    employee.isLocked() ? "wrong password; account locked" : "wrong password");
            throw new AuthenticationException(employee.isLocked() ? ACCOUNT_LOCKED : INVALID_CREDENTIALS);
        }

        employee.recordSuccessfulLogin();
        employees.update(employee);
        session.start(employee);
        session.audit(AuditAction.LOGIN_SUCCEEDED, null);
        return employee;
    }

    public void logout() {
        if (session.isActive()) {
            session.audit(AuditAction.LOGGED_OUT, null);
            session.end();
        }
    }

    public Employee currentEmployee() {
        return session.employee();
    }

    /** Normal password change: the employee proves who they are with the current password. */
    public void changePassword(char[] currentPassword, char[] newPassword) {
        Employee me = session.employee();
        if (!hasher.matches(currentPassword, me.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        applyNewPassword(me, newPassword);
    }

    /** Straight after logging in with a temporary password. */
    public void completeRequiredPasswordChange(char[] newPassword) {
        Employee me = session.employee();
        if (!me.isPasswordChangeRequired()) {
            throw new IllegalStateException("No password change is pending");
        }
        applyNewPassword(me, newPassword);
    }

    private void applyNewPassword(Employee me, char[] newPassword) {
        requireStrongEnough(newPassword);
        if (hasher.matches(newPassword, me.getPasswordHash())) {
            throw new IllegalArgumentException("The new password must be different from the current one");
        }
        me.changePassword(hasher.hash(newPassword));
        employees.update(me);
        session.audit(AuditAction.PASSWORD_CHANGED, null);
    }

    /** Package-private so EmployeeService applies the same rule to temporary passwords. */
    static void requireStrongEnough(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}