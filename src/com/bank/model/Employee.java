package com.bank.model;

import java.util.Objects;

public final class Employee {

    public static final int MAX_FAILED_LOGINS = 5;

    private final String employeeId;
    private final String fullName;
    private Role role;
    private EmployeeStatus status;
    private String passwordHash;
    private boolean passwordChangeRequired;
    private int failedLogins;

    private Employee(String employeeId, String fullName, Role role, EmployeeStatus status,
                     String passwordHash, boolean passwordChangeRequired, int failedLogins) {
        this.employeeId = requireNonBlank(employeeId, "Employee ID");
        this.fullName = requireNonBlank(fullName, "Full name");
        this.role = Objects.requireNonNull(role, "Role is required");
        this.status = Objects.requireNonNull(status, "Status is required");
        this.passwordHash = requireNonBlank(passwordHash, "Password hash");
        if (failedLogins < 0) {
            throw new IllegalArgumentException("Failed login count cannot be negative");
        }
        this.passwordChangeRequired = passwordChangeRequired;
        this.failedLogins = failedLogins;
    }

    /** A new employee: active, with a temporary password they must change at first login. */
    public static Employee hire(String employeeId, String fullName, Role role, String passwordHash) {
        return new Employee(employeeId, fullName, role, EmployeeStatus.ACTIVE,
                passwordHash, true, 0);
    }

    /** Rebuilds an existing employee from storage. */
    public static Employee restore(String employeeId, String fullName, Role role,
                                   EmployeeStatus status, String passwordHash,
                                   boolean passwordChangeRequired, int failedLogins) {
        return new Employee(employeeId, fullName, role, status,
                passwordHash, passwordChangeRequired, failedLogins);
    }

    // ---------- Permissions ----------

    /** Only an active employee can do anything, whatever their role says. */
    public boolean can(Permission permission) {
        return status == EmployeeStatus.ACTIVE && role.allows(permission);
    }

    // ---------- Login bookkeeping ----------

    public void recordFailedLogin() {
        failedLogins++;
        if (failedLogins >= MAX_FAILED_LOGINS && status == EmployeeStatus.ACTIVE) {
            status = EmployeeStatus.LOCKED;
        }
    }

    public void recordSuccessfulLogin() {
        failedLogins = 0;
    }

    // ---------- Changes ----------

    public void changePassword(String newPasswordHash) {
        this.passwordHash = requireNonBlank(newPasswordHash, "Password hash");
        this.passwordChangeRequired = false;
    }

    /** Sets a temporary password, unlocks the account, and forces a change at next login. */
    public void resetPassword(String temporaryPasswordHash) {
        requireNotInactive();
        this.passwordHash = requireNonBlank(temporaryPasswordHash, "Password hash");
        this.passwordChangeRequired = true;
        this.failedLogins = 0;
        this.status = EmployeeStatus.ACTIVE;
    }

    public void changeRole(Role newRole) {
        requireNotInactive();
        this.role = Objects.requireNonNull(newRole, "Role is required");
    }

    public void deactivate() {
        requireNotInactive();
        status = EmployeeStatus.INACTIVE;
    }

    // ---------- Getters ----------

    public String getEmployeeId() { return employeeId; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public EmployeeStatus getStatus() { return status; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public int getFailedLogins() { return failedLogins; }

    public boolean isActive() { return status == EmployeeStatus.ACTIVE; }
    public boolean isLocked() { return status == EmployeeStatus.LOCKED; }

    // ---------- Helpers ----------

    private void requireNotInactive() {
        if (status == EmployeeStatus.INACTIVE) {
            throw new IllegalStateException("Employee " + employeeId + " is inactive");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    // ---------- Identity ----------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee other)) return false;
        return employeeId.equals(other.employeeId);
    }

    @Override
    public int hashCode() {
        return employeeId.hashCode();
    }

    /** Deliberately leaves out the password hash: toString ends up in logs. */
    @Override
    public String toString() {
        return "%s | %s | %s | %s".formatted(employeeId, fullName, role, status);
    }
}