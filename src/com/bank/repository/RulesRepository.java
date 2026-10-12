package com.bank.repository;

import com.bank.config.BankRules;

import java.util.Optional;

public interface RulesRepository {

    /** The saved rules, or empty if none have been saved yet. */
    Optional<BankRules> load();

    void save(BankRules rules);
}