package com.bank.ui;

import com.bank.model.AccountStatus;
import com.bank.service.BranchReport;
import com.bank.service.ReportService;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReportScreen {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReportService reportService;

    public ReportScreen(ReportService reportService) {
        this.reportService = Objects.requireNonNull(reportService);
    }

    public void show() {
        BranchReport report = reportService.branchReport();

        System.out.println("\n=== Branch Report - " + WHEN.format(report.generatedAt()) + " ===");
        System.out.printf("Customers:              %d active, %d inactive%n",
                report.activeCustomers(), report.inactiveCustomers());

        String statuses = Arrays.stream(AccountStatus.values())
                .map(status -> status + " " + report.accountsByStatus().getOrDefault(status, 0L))
                .collect(Collectors.joining(" | "));
        System.out.println("Accounts:               " + statuses);

        System.out.println("Open accounts by type:");
        if (report.openAccountsByType().isEmpty()) {
            System.out.println("  (none)");
        }
        report.openAccountsByType().forEach((type, summary) -> System.out.printf(
                "  %-14s %4d account(s)   %,16.2f%n", type, summary.count(), summary.balance()));

        System.out.printf("Total deposits held:    %,16.2f%n", report.totalDeposits());
        System.out.printf("Total overdrafts owed:  %,16.2f%n", report.totalOverdrawn());
        System.out.println("Loans:                  available once the Loans module is built");
    }
}