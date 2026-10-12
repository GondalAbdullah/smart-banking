package com.bank.service;

import com.bank.config.BankRules;
import com.bank.model.AuditAction;
import com.bank.model.Permission;
import com.bank.repository.RulesRepository;
import com.bank.util.RecordCodec;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;

/** Holds the banking rules in force, and lets an administrator change them one at a time. */
public class RulesService {

    private final RulesRepository repository;
    private final Session session;
    private final RecordCodec<BankRules> codec = RecordCodec.of(BankRules.class);
    private BankRules current;

    /** Loads the saved rules; on the very first run, saves the defaults so the file exists. */
    public RulesService(RulesRepository repository, Session session) {
        this.repository = Objects.requireNonNull(repository);
        this.session = Objects.requireNonNull(session);
        this.current = repository.load().orElseGet(() -> {
            BankRules defaults = BankRules.defaults();
            repository.save(defaults);
            return defaults;
        });
    }

    /** The rules in force right now. AccountService receives this as rulesService::current. */
    public BankRules current() {
        return current;
    }

    /** Every rule as name -> value, in declaration order. */
    public Map<String, String> describe() {
        return codec.toStrings(current);
    }

    public BankRules update(String ruleName, String newValue) {
        session.require(Permission.CONFIGURE_RULES);
        // Checks the name exists, parses the value, and runs BankRules' own validation.
        BankRules updated = codec.with(current, ruleName, newValue);
        try {
            repository.save(updated);   // save first: if this fails, nothing has changed
        } catch (UncheckedIOException e) {
            throw new IllegalStateException(
                    "Could not save the banking rules: " + e.getCause().getMessage(), e);
        }
        String oldValue = describe().get(ruleName);
        current = updated;
        session.audit(AuditAction.RULES_CHANGED,
                ruleName + ": " + oldValue + " -> " + describe().get(ruleName));
        return updated;
    }
}