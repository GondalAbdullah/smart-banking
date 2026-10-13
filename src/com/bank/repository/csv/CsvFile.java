package com.bank.repository.csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * One CSV file with a fixed header. The repositories use it in two ways:
 * <ul>
 *   <li>writeAll: rewrite the whole file, for data that changes (customers, accounts...)</li>
 *   <li>append: add one line at the end, for data that is never edited (transactions, audit)</li>
 * </ul>
 * The header is checked on every read, so a file whose columns were moved or renamed is refused
 * instead of being read into the wrong fields.
 */
final class CsvFile {

    private final Path path;
    private final List<String> header;
    private final Map<String, Integer> columns = new HashMap<>();

    CsvFile(Path path, String... header) {
        this.path = Objects.requireNonNull(path);
        this.header = List.of(header);
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i], i);
        }
    }

    // ---------- Reading ----------

    /** Reads every line after the header with the given mapper. A missing file means no data yet. */
    <T> List<T> readAll(Function<CsvRow, T> mapper) {
        if (Files.notExists(path)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
        if (lines.isEmpty()) {
            return List.of();
        }
        checkHeader(lines.getFirst());

        List<T> items = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            int lineNumber = i + 1;   // people count lines from 1, and the header is line 1
            CsvRow row = new CsvRow(path, lineNumber, columns, fields(line, lineNumber));
            try {
                items.add(mapper.apply(row));
            } catch (IllegalArgumentException e) {
                // e.g. Customer.restore refusing an invalid email: say where it came from
                throw row.error(e.getMessage());
            }
        }
        return items;
    }

    private void checkHeader(String firstLine) {
        String withoutBom = firstLine.startsWith("﻿") ? firstLine.substring(1) : firstLine;
        if (!withoutBom.equals(Csv.join(header))) {
            throw new IllegalStateException(path + " has an unexpected header. Expected: " + Csv.join(header));
        }
    }

    private List<String> fields(String line, int lineNumber) {
        List<String> fields;
        try {
            fields = Csv.split(line);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(path + " line " + lineNumber + ": " + e.getMessage());
        }
        if (fields.size() != header.size()) {
            throw new IllegalStateException(path + " line " + lineNumber + ": has " + fields.size()
                    + " fields, expected " + header.size());
        }
        return fields;
    }

    // ---------- Writing ----------

    /**
     * Replaces the whole file. Writes a temporary file first, then swaps it in,
     * so a crash halfway through never leaves a half-written file behind.
     */
    <T> void writeAll(Collection<T> items, Function<T, Map<String, Object>> mapper) {
        List<String> lines = new ArrayList<>();
        lines.add(Csv.join(header));
        for (T item : items) {
            lines.add(toLine(mapper.apply(item)));
        }
        try {
            Path folder = path.toAbsolutePath().getParent();
            Files.createDirectories(folder);
            Path temp = Files.createTempFile(folder, path.getFileName().toString(), ".tmp");
            try {
                Files.write(temp, lines, StandardCharsets.UTF_8);
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + path, e);
        }
    }

    /** Adds one line at the end, writing the header first if the file is new. */
    void append(Map<String, Object> values) {
        List<String> lines = new ArrayList<>();
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            if (Files.notExists(path) || Files.size(path) == 0) {
                lines.add(Csv.join(header));
            }
            lines.add(toLine(values));
            Files.write(path, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + path, e);
        }
    }

    /** Puts the values in header order. A column the map leaves out is written as empty. */
    private String toLine(Map<String, Object> values) {
        for (String name : values.keySet()) {
            if (!columns.containsKey(name)) {
                throw new IllegalArgumentException("Unknown column '" + name + "' for " + path.getFileName());
            }
        }
        return Csv.join(header.stream()
                .map(column -> Csv.text(values.get(column)))
                .toList());
    }
}
