package com.bank.ui;

import com.bank.exception.NotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class Menu {

    private final String title;
    private final ConsoleInput input;
    private final Map<String, MenuOption> options = new LinkedHashMap<>();

    Menu(String title, ConsoleInput input) {
        this.title = Objects.requireNonNull(title);
        this.input = Objects.requireNonNull(input);
    }

    /** Adds an option numbered automatically 1, 2, 3... Returns this menu for chaining. */
    Menu option(String label, Runnable action) {
        String key = String.valueOf(options.size() + 1);
        options.put(key, new MenuOption(label, action));
        return this;
    }

    void show() {
        while (true) {
            System.out.println("\n--- " + title + " ---");
            options.forEach((key, option) -> System.out.println(key + ". " + option.label()));
            System.out.println("0. Back");

            String choice = input.readLine("Choose: ");
            if (choice.equals("0")) {
                return;
            }

            MenuOption option = options.get(choice);
            if (option == null) {
                System.out.println("Invalid option, try again.");
                continue;
            }
            runSafely(option.action());
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}