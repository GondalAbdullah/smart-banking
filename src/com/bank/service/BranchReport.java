package com.bank.service;

import com.bank.model.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** A snapshot of the branch. Built by ReportService; the UI only prints it. */
public record BranchReport(
        LocalDateTime generatedAt,
        long activeCustomers,
        long inactiveCustomers,
        Map<AccountStatus, Long> accountsByStatus,
        Map<String, TypeSummary> openAccountsByType,
        BigDecimal totalDeposits,
        BigDecimal totalOverdrawn) {

    /** How many open accounts of one type there are, and what they hold together. */
    public record TypeSummary(long count, BigDecimal balance) {
    }
}