package com.bank.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** One sensitive action: who did what, when. Never edited, never deleted. */
public record AuditEntry(LocalDateTime timestamp, String employeeId, AuditAction action,
                         String details) {

    public AuditEntry {
        Objects.requireNonNull(timestamp, "Timestamp is required");
        Objects.requireNonNull(employeeId, "Employee ID is required");
        Objects.requireNonNull(action, "Action is required");
        details = (details == null) ? "" : details;
    }
}