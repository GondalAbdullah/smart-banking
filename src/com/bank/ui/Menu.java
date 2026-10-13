package com.bank.ui;

import com.bank.exception.BankException;
import com.bank.model.Permission;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

final class Menu {

    private final String title;
    private final ConsoleInput input;
    private final Predicate<Permission> allowed;
    private final List<MenuOption> options = new ArrayList<>();
    private String exitLabel = "Back";

    Menu(String title, ConsoleInput input, Predicate<Permission> allowed) {
        this.title = Objects.requireNonNull(title);
        this.input = Objects.requireNonNull(input);
        this.allowed = Objects.requireNonNull(allowed);
    }

    Menu option(String label, Runnable action) {
        return option(label, null, action);
    }

    Menu option(String label, Permission required, Runnable action) {
        options.add(new MenuOption(label, required, action));
        return this;
    }

    Menu exitLabel(String label) {
        this.exitLabel = Objects.requireNonNull(label);
        return this;
    }

    void show() {
        while (true) {
            List<MenuOption> visible = options.stream()
                    .filter(option -> option.isAllowed(allowed))
                    .toList();

            System.out.println("\n--- " + title + " ---");
            for (int i = 0; i < visible.size(); i++) {
                System.out.println((i + 1) + ". " + visible.get(i).label());
            }
            System.out.println("0. " + exitLabel);

            String choice = input.readLine("Choose: ");
            if (choice.equals("0")) {
                return;
            }
            MenuOption option = pick(visible, choice);
            if (option == null) {
                System.out.println("Invalid option, try again.");
                continue;
            }
            runSafely(option.action());
        }
    }

    private static MenuOption pick(List<MenuOption> visible, String choice) {
        try {
            int index = Integer.parseInt(choice);
            return (index >= 1 && index <= visible.size()) ? visible.get(index - 1) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | BankException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (UncheckedIOException e) {
            System.out.println("Error: " + e.getMessage() + " (" + e.getCause().getMessage() + ")");
        }
    }
}
