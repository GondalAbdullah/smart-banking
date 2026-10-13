package com.bank.repository.csv;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One line of a CSV file, read by column name instead of position.
 * Every parse error names the file, the line and the column, so a broken file is easy to fix.
 */
final class CsvRow {

    private final Path file;
    private final int lineNumber;
    private final Map<String, Integer> columns;
    private final List<String> values;

    CsvRow(Path file, int lineNumber, Map<String, Integer> columns, List<String> values) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.columns = columns;
        this.values = values;
    }

    String text(String column) {
        Integer index = columns.get(column);
        if (index == null) {
            throw new IllegalArgumentException("No column named '" + column + "' in " + file.getFileName());
        }
        return values.get(index);
    }

    /** An empty field means "no value". */
    String optional(String column) {
        String value = text(column);
        return value.isEmpty() ? null : value;
    }

    BigDecimal decimal(String column) {
        return parse(column, BigDecimal::new, "number");
    }

    int integer(String column) {
        return parse(column, Integer::parseInt, "whole number");
    }

    boolean bool(String column) {
        return parse(column, text -> switch (text) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException();
        }, "true/false value");
    }

    LocalDate date(String column) {
        return parse(column, LocalDate::parse, "date");
    }

    LocalDateTime dateTime(String column) {
        return parse(column, LocalDateTime::parse, "date and time");
    }

    YearMonth yearMonth(String column) {
        return parse(column, YearMonth::parse, "year and month");
    }

    <E extends Enum<E>> E enumValue(String column, Class<E> type) {
        return parse(column, text -> Enum.valueOf(type, text), type.getSimpleName());
    }

    /** An error that points at this line, e.g. "data/accounts.csv line 7: ...". */
    IllegalStateException error(String problem) {
        return new IllegalStateException(file + " line " + lineNumber + ": " + problem);
    }

    private <T> T parse(String column, Function<String, T> parser, String expected) {
        String value = text(column);
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw error(column + " '" + value + "' is not a valid " + expected);
        }
    }
}
