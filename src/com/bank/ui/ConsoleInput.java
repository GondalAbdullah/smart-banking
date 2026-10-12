package com.bank.ui;

import java.math.BigDecimal;
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

    /** Keeps asking until the input is a number. Whether the amount is VALID is the model's job. */
    public BigDecimal readAmount(String prompt) {
        while (true) {
            String text = readRequired(prompt);
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number, e.g. 2500 or 2500.50");
            }
        }
    }

    public int readPositiveInt(String prompt) {
        while (true) {
            String text = readRequired(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // falls through to the message below
            }
            System.out.println("Please enter a whole number greater than zero");
        }
    }
}

