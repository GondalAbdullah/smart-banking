package com.bank.repository;

import com.bank.model.AuditEntry;

import java.util.List;

/** Append-only, like transactions: an audit trail you can edit is not an audit trail. */
public interface AuditRepository {

    void add(AuditEntry entry);

    /** All entries, oldest first. */
    List<AuditEntry> findAll();
}