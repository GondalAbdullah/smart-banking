package com.bank.repository;

import com.bank.model.AuditEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InMemoryAuditRepository implements AuditRepository {

    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void add(AuditEntry entry) {
        entries.add(Objects.requireNonNull(entry, "Audit entry is required"));
    }

    @Override
    public List<AuditEntry> findAll() {
        return List.copyOf(entries);
    }
}