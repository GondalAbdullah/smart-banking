package com.bank.service;

import com.bank.model.Account;
import com.bank.model.AccountStatus;
import com.bank.model.Customer;
import com.bank.model.Permission;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Read-only summaries of the branch. Never changes anything. */
public class ReportService {

    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final Session session;
    private final Clock clock;

    public ReportService(CustomerRepository customers, AccountRepository accounts,
                         Session session, Clock clock) {
        this.customers = Objects.requireNonNull(customers);
        this.accounts = Objects.requireNonNull(accounts);
        this.session = Objects.requireNonNull(session);
        this.clock = Objects.requireNonNull(clock);
    }

    public BranchReport branchReport() {
        session.require(Permission.VIEW_BRANCH_REPORT);

        // partitioningBy always returns both keys, true and false, even when a side is empty.
        Map<Boolean, Long> customersByActive = customers.findAll().stream()
                .collect(Collectors.partitioningBy(Customer::isActive, Collectors.counting()));

        List<Account> all = accounts.findAll();

        // An EnumMap keeps the statuses in declaration order: ACTIVE, FROZEN, CLOSED.
        Map<AccountStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(Account::getStatus,
                        () -> new EnumMap<>(AccountStatus.class),
                        Collectors.counting()));

        List<Account> open = all.stream()
                .filter(account -> !account.isClosed())
                .toList();

        // teeing runs two collectors over the same elements and combines their results.
        Map<String, BranchReport.TypeSummary> byType = open.stream()
                .collect(Collectors.groupingBy(Account::typeLabel, TreeMap::new,
                        Collectors.teeing(
                                Collectors.counting(),
                                Collectors.reducing(BigDecimal.ZERO, Account::getBalance, BigDecimal::add),
                                BranchReport.TypeSummary::new)));

        BigDecimal deposits = sumOfBalances(open, balance -> balance.signum() > 0);
        BigDecimal overdrawn = sumOfBalances(open, balance -> balance.signum() < 0).negate();

        return new BranchReport(
                LocalDateTime.now(clock),
                customersByActive.get(true),
                customersByActive.get(false),
                Collections.unmodifiableMap(byStatus),
                Collections.unmodifiableMap(byType),
                deposits,
                overdrawn);
    }

    private static BigDecimal sumOfBalances(List<Account> accounts, Predicate<BigDecimal> which) {
        return accounts.stream()
                .map(Account::getBalance)
                .filter(which)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}