package com.bank.ui;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.io.Console;
import java.util.Arrays;

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
        return choose(prompt, enumType, true);
    }

    /** Like chooseOptional, but keeps asking until the user picks something. */
    public <E extends Enum<E>> E chooseRequired(String prompt, Class<E> enumType) {
        return choose(prompt, enumType, false);
    }

    private <E extends Enum<E>> E choose(String prompt, Class<E> enumType, boolean optional) {
        E[] choices = enumType.getEnumConstants();
        for (int i = 0; i < choices.length; i++) {
            System.out.println((i + 1) + ". " + choices[i]);
        }
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty() && optional) {
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
            System.out.println("Choose 1 to " + choices.length
                    + (optional ? ", or press Enter for any" : ""));
        }
    }

    /**
     * Reads a password as char[] so the caller can wipe it afterwards.
     * In a real terminal the typing is hidden; IDE consoles have no System.console(), so it shows.
     */
    public char[] readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword(prompt);
            return (password == null) ? new char[0] : password;
        }
        return readLine(prompt).toCharArray();
    }

    /** Asks twice until both entries match. Whether it is strong enough is the service's job. */
    public char[] readNewPassword(String prompt) {
        while (true) {
            char[] first = readPassword(prompt);
            char[] second = readPassword("Confirm password: ");
            boolean same = Arrays.equals(first, second);
            Arrays.fill(second, '\0');
            if (same) {
                return first;
            }
            Arrays.fill(first, '\0');
            System.out.println("The two entries do not match. Try again.");
        }
    }
}

