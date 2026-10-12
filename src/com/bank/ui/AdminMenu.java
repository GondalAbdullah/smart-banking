package com.bank.ui;

import com.bank.model.AuditEntry;
import com.bank.model.Employee;
import com.bank.model.Permission;
import com.bank.model.Role;
import com.bank.service.EmployeeService;
import com.bank.service.RulesService;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class AdminMenu {

    private static final String EMPLOYEE_ROW = "%-6s %-24s %-20s %-9s %s%n";
    private static final String AUDIT_ROW = "%-17s %-8s %-24s %s%n";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int AUDIT_ROWS = 20;

    private final EmployeeService employeeService;
    private final RulesService rulesService;
    private final ConsoleInput input;
    private final Menu menu;

    public AdminMenu(EmployeeService employeeService, RulesService rulesService,
                     ConsoleInput input, Predicate<Permission> allowed) {
        this.employeeService = employeeService;
        this.rulesService = rulesService;
        this.input = input;
        this.menu = new Menu("Administration", input, allowed)
                .option("Hire employee", Permission.MANAGE_EMPLOYEES, this::hire)
                .option("List employees", Permission.MANAGE_EMPLOYEES, this::listEmployees)
                .option("Change employee role", Permission.MANAGE_EMPLOYEES, this::changeRole)
                .option("Reset employee password", Permission.MANAGE_EMPLOYEES, this::resetPassword)
                .option("Deactivate employee", Permission.MANAGE_EMPLOYEES, this::deactivate)
                .option("View audit trail", Permission.VIEW_AUDIT_LOG, this::auditTrail)
                .option("View banking rules", Permission.CONFIGURE_RULES, this::viewRules)
                .option("Change a banking rule", Permission.CONFIGURE_RULES, this::changeRule);
    }

    public void show() {
        menu.show();
    }

    private void hire() {
        String fullName = input.readRequired("Full name: ");
        Role role = input.chooseRequired("Role: ", Role.class);
        char[] temporary = input.readNewPassword("Temporary password: ");
        try {
            Employee employee = employeeService.hire(fullName, role, temporary);
            System.out.println("Hired " + employee.getEmployeeId() + " as " + role.label()
                    + ". They must change the password at first login.");
        } finally {
            Arrays.fill(temporary, '\0');
        }
    }

    private void listEmployees() {
        List<Employee> employees = employeeService.listAll();
        System.out.printf(EMPLOYEE_ROW, "ID", "Name", "Role", "Status", "");
        employees.forEach(e -> System.out.printf(EMPLOYEE_ROW,
                e.getEmployeeId(), e.getFullName(), e.getRole().label(), e.getStatus(),
                e.isPasswordChangeRequired() ? "(temporary password)" : ""));
    }

    private void changeRole() {
        Employee employee = employeeService.getById(input.readRequired("Employee ID: "));
        System.out.println(employee.getFullName() + " is currently a " + employee.getRole().label());
        Role newRole = input.chooseRequired("New role: ", Role.class);
        employeeService.changeRole(employee.getEmployeeId(), newRole);
        System.out.println("Role changed to " + newRole.label() + ".");
    }

    private void resetPassword() {
        Employee employee = employeeService.getById(input.readRequired("Employee ID: "));
        char[] temporary = input.readNewPassword("Temporary password: ");
        try {
            employeeService.resetPassword(employee.getEmployeeId(), temporary);
            System.out.println("Password reset; " + employee.getEmployeeId()
                    + " is unlocked and must change it at next login.");
        } finally {
            Arrays.fill(temporary, '\0');
        }
    }

    private void deactivate() {
        Employee employee = employeeService.getById(input.readRequired("Employee ID: "));
        if (!input.confirm("Deactivate " + employee.getFullName() + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        employeeService.deactivate(employee.getEmployeeId());
        System.out.println("Employee " + employee.getEmployeeId() + " deactivated.");
    }

    private void auditTrail() {
        List<AuditEntry> entries = employeeService.auditTrail(AUDIT_ROWS);
        if (entries.isEmpty()) {
            System.out.println("The audit trail is empty.");
            return;
        }
        System.out.println("Latest " + entries.size() + " entries, newest first:");
        System.out.printf(AUDIT_ROW, "When", "Who", "Action", "Details");
        entries.forEach(e -> System.out.printf(AUDIT_ROW,
                WHEN.format(e.timestamp()), e.employeeId(), e.action(), e.details()));
    }

        private void viewRules() {
        printRules(rulesService.describe());
        System.out.println("Rates are fractions: 0.04 means 4%.");
    }

    private void changeRule() {
        Map<String, String> rules = rulesService.describe();
        List<String> names = List.copyOf(rules.keySet());
        printRules(rules);

        int choice = input.readPositiveInt("Rule to change: ");
        if (choice > names.size()) {
            System.out.println("There is no rule " + choice + ".");
            return;
        }
        String name = names.get(choice - 1);
        String oldValue = rules.get(name);
        String newValue = input.readRequired("New value for " + label(name) + " [" + oldValue + "]: ");

        if (!input.confirm("Change " + label(name) + " from " + oldValue + " to " + newValue + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        rulesService.update(name, newValue);
        System.out.println("Saved. It applies to accounts opened from now on; existing accounts keep their terms.");
    }

    private static void printRules(Map<String, String> rules) {
        int number = 1;
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            System.out.printf("%d. %-34s %s%n", number++, label(rule.getKey()), rule.getValue());
        }
    }

    /** savingsMinimumBalance -> "Savings minimum balance" */
    private static String label(String name) {
        String spaced = name.replaceAll("([A-Z])", " $1").toLowerCase();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}