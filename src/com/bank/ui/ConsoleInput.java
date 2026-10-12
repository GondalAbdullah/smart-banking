package com.bank.ui;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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

        /** Empty input returns null, meaning "no filter". */
    public LocalDate readOptionalDate(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                System.out.println("Please use the format YYYY-MM-DD, e.g. 2026-10-12");
            }
        }
    }

    /** Lists the constants of any enum and lets the user pick one. Empty input returns null. */
    public <E extends Enum<E>> E chooseOptional(String prompt, Class<E> enumType) {
        E[] choices = enumType.getEnumConstants();
        for (int i = 0; i < choices.length; i++) {
            System.out.println((i + 1) + ". " + choices[i]);
        }
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return null;
            }
            try {
                int index = Integer.parseInt(text);
                if (index >= 1 && index <= choices.length) {
                    return choices[index - 1];
                }
            } catch (NumberFormatException ignored) {
                // falls through to the message below
            }
            System.out.println("Choose 1 to " + choices.length + ", or press Enter for any");
        }
    }
}

