package com.bank.testsupport;

import com.bank.config.BankRules;
import com.bank.repository.RulesRepository;

import java.util.Optional;

/** Keeps the rules in memory, so most tests never touch the disk. */
public final class InMemoryRulesRepository implements RulesRepository {

    private BankRules saved;

    @Override
    public Optional<BankRules> load() {
        return Optional.ofNullable(saved);
    }

    @Override
    public void save(BankRules rules) {
        saved = rules;
    }
}