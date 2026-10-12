package com.bank.model;

/** One thing an employee may be allowed to do. Roles are just named sets of these. */
public enum Permission {

    REGISTER_CUSTOMER("register customers"),
    UPDATE_CUSTOMER("update customer details"),
    DEACTIVATE_CUSTOMER("deactivate customers"),
    OPEN_ACCOUNT("open accounts"),
    PROCESS_TRANSACTIONS("process deposits, withdrawals and transfers"),
    MANAGE_ACCOUNT_STATUS("freeze, unfreeze or close accounts"),
    APPLY_INTEREST("apply monthly interest"),
    APPROVE_LOANS("approve or reject loans"),
    VIEW_BRANCH_REPORT("view branch reports"),
    VIEW_AUDIT_LOG("view the audit trail"),
    MANAGE_EMPLOYEES("manage employees"),
    CONFIGURE_RULES("configure banking rules");

    private final String description;

    Permission(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}