package com.bank.ui;

import com.bank.model.Employee;
import com.bank.model.Permission;
import com.bank.service.AuthService;

import java.util.Arrays;
import java.util.function.Predicate;

public class MainMenu {

    private final AuthService authService;
    private final CustomerMenu customerMenu;
    private final AccountMenu accountMenu;
    private final TransactionMenu transactionMenu;
    private final LoanMenu loanMenu;
    private final AdminMenu adminMenu;
    private final ReportScreen reportScreen;
    private final ConsoleInput input;
    private final Predicate<Permission> allowed;

    public MainMenu(AuthService authService, CustomerMenu customerMenu, AccountMenu accountMenu,
                    TransactionMenu transactionMenu, LoanMenu loanMenu, AdminMenu adminMenu,
                    ReportScreen reportScreen, ConsoleInput input, Predicate<Permission> allowed) {
        this.authService = authService;
        this.customerMenu = customerMenu;
        this.accountMenu = accountMenu;
        this.transactionMenu = transactionMenu;
        this.loanMenu = loanMenu;
        this.adminMenu = adminMenu;
        this.reportScreen = reportScreen;
        this.input = input;
        this.allowed = allowed;
    }

    /** Runs until the employee logs out. */
    public void show() {
        Employee me = authService.currentEmployee();
        new Menu("Main Menu - " + me.getFullName() + ", " + me.getRole().label(), input, allowed)
                .option("Customers", customerMenu::show)
                .option("Accounts", accountMenu::show)
                .option("Transactions", transactionMenu::show)
                .option("Loans", loanMenu::show)
                .option("Branch report", Permission.VIEW_BRANCH_REPORT, reportScreen::show)
                .option("Administration", Permission.MANAGE_EMPLOYEES, adminMenu::show)
                .option("Change my password", this::changePassword)
                .exitLabel("Log out")
                .show();

        authService.logout();
        System.out.println("Logged out.");
    }

    private void changePassword() {
        char[] current = input.readPassword("Current password: ");
        char[] newPassword = input.readNewPassword("New password: ");
        try {
            authService.changePassword(current, newPassword);
            System.out.println("Password changed.");
        } finally {
            Arrays.fill(current, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }
}