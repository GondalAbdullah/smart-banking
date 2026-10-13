package com.bank.repository.csv;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns one line of CSV into fields and back. One record per line.
 * A field is wrapped in quotes when it contains a comma or a quote, and a quote inside it is doubled:
 * <pre>
 *   Lahore             ->  Lahore
 *   House 5, Street 2  ->  "House 5, Street 2"
 *   The "Mall"         ->  "The ""Mall"""
 * </pre>
 */
final class Csv {

    private static final char SEPARATOR = ',';
    private static final char QUOTE = '"';

    private Csv() {
        // static helpers only
    }

    static String join(List<String> fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                line.append(SEPARATOR);
            }
            line.append(escape(fields.get(i)));
        }
        return line.toString();
    }

    /** The reverse of join. Throws IllegalArgumentException if the quotes are broken. */
    static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c != QUOTE) {
                    field.append(c);                       // inside quotes, commas are just text
                } else if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                    field.append(QUOTE);                   // "" inside quotes is one real quote
                    i++;
                } else {
                    inQuotes = false;                      // the closing quote
                    if (i + 1 < line.length() && line.charAt(i + 1) != SEPARATOR) {
                        throw new IllegalArgumentException(
                                "unexpected text after a closing quote at position " + (i + 2));
                    }
                }
            } else if (c == SEPARATOR) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == QUOTE && field.isEmpty()) {
                inQuotes = true;                           // an opening quote
            } else {
                field.append(c);
            }
            i++;
        }
        if (inQuotes) {
            throw new IllegalArgumentException("a quoted field is never closed");
        }
        fields.add(field.toString());
        return fields;
    }

    /** How a value is written: null becomes an empty field, and a BigDecimal never uses 1E+3 notation. */
    static String text(Object value) {
        return switch (value) {
            case null -> "";
            case BigDecimal decimal -> decimal.toPlainString();
            default -> value.toString();
        };
    }

    private static String escape(String value) {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("A CSV field cannot contain a line break: " + value);
        }
        if (value.indexOf(SEPARATOR) < 0 && value.indexOf(QUOTE) < 0) {
            return value;
        }
        return QUOTE + value.replace("\"", "\"\"") + QUOTE;
    }
}
