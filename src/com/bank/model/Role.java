package com.bank.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.bank.model.Permission.*;

public enum Role {

    TELLER("Bank Teller", EnumSet.of(
            REGISTER_CUSTOMER, UPDATE_CUSTOMER, OPEN_ACCOUNT, PROCESS_TRANSACTIONS)),

    MANAGER("Bank Manager", including(TELLER, EnumSet.of(
            DEACTIVATE_CUSTOMER, MANAGE_ACCOUNT_STATUS, APPLY_INTEREST,
            APPROVE_LOANS, VIEW_BRANCH_REPORT))),

    ADMIN("Bank Administrator", EnumSet.of(
            MANAGE_EMPLOYEES, CONFIGURE_RULES, APPLY_INTEREST,
            VIEW_BRANCH_REPORT, VIEW_AUDIT_LOG));

    private final String label;
    private final Set<Permission> permissions;

    Role(String label, EnumSet<Permission> permissions) {
        this.label = label;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public boolean allows(Permission permission) {
        return permissions.contains(permission);
    }

    public String label() {
        return label;
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    /** A role's permissions plus some extra ones. */
    private static EnumSet<Permission> including(Role base, EnumSet<Permission> extra) {
        EnumSet<Permission> all = EnumSet.copyOf(base.permissions);
        all.addAll(extra);
        return all;
    }
}