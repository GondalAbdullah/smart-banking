package com.bank.ui;

import java.util.Objects;
import java.util.Scanner;

public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "Scanner is required");
    }

    /** Reads one line; may return an empty string. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Keeps asking until the user types something. */
    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field is required.");
        }
    }

    public boolean confirm(String prompt) {
        String answer = readLine(prompt + " (y/n): ");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }
}