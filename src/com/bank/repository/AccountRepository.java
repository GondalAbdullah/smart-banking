package com.bank.repository;

import com.bank.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    void add(Account account);

    void update(Account account);

    Optional<Account> findByNumber(String accountNumber);

    List<Account> findByCustomerId(String customerId);

    List<Account> findAll();

    String nextAccountNumber();
}