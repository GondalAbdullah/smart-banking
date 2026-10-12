package com.bank.service;

import com.bank.model.AuditAction;
import com.bank.model.AuditEntry;
import com.bank.repository.AuditRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Writes and reads the audit trail. Package-private methods: only services may use it. */
public class AuditLog {

    /** Used as the "employee" for actions the system takes by itself. */
    static final String SYSTEM = "SYSTEM";

    private final AuditRepository repository;
    private final Clock clock;

    public AuditLog(AuditRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    void record(String employeeId, AuditAction action, String details) {
        repository.add(new AuditEntry(LocalDateTime.now(clock), employeeId, action, details));
    }

    /** The most recent entries, newest first. */
    List<AuditEntry> recent(int limit) {
        return repository.findAll().reversed().stream()
                .limit(limit)
                .toList();
    }
}