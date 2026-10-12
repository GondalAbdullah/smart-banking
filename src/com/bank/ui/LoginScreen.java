package com.bank.ui;

import com.bank.exception.AuthenticationException;
import com.bank.model.Employee;
import com.bank.service.AuthService;

import java.util.Arrays;
import java.util.Objects;

public class LoginScreen {

    private final AuthService authService;
    private final ConsoleInput input;

    public LoginScreen(AuthService authService, ConsoleInput input) {
        this.authService = Objects.requireNonNull(authService);
        this.input = Objects.requireNonNull(input);
    }

    /** Returns true once someone has logged in, false if the user chose to exit. */
    public boolean show() {
        while (true) {
            System.out.println("\n=== Smart Banking System - Staff Login ===");
            String employeeId = input.readLine("Employee ID (0 to exit): ");
            if (employeeId.equals("0")) {
                return false;
            }
            if (employeeId.isEmpty()) {
                continue;
            }
            char[] password = input.readPassword("Password: ");
            try {
                Employee employee = authService.login(employeeId, password);
                System.out.println("Welcome, " + employee.getFullName()
                        + " (" + employee.getRole().label() + ").");
                if (employee.isPasswordChangeRequired()) {
                    requirePasswordChange();
                }
                return true;
            } catch (AuthenticationException e) {
                System.out.println(e.getMessage());
            } finally {
                Arrays.fill(password, '\0');   // don't leave the password lying in memory
            }
        }
    }

    private void requirePasswordChange() {
        System.out.println("You are using a temporary password. Please choose a new one.");
        while (true) {
            char[] newPassword = input.readNewPassword("New password: ");
            try {
                authService.completeRequiredPasswordChange(newPassword);
                System.out.println("Password changed.");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } finally {
                Arrays.fill(newPassword, '\0');
            }
        }
    }
}