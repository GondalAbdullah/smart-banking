package com.bank.repository;

import com.bank.config.BankRules;
import com.bank.util.RecordCodec;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Keeps the banking rules in a .properties file, one "name=value" line per rule. */
public class PropertiesRulesRepository implements RulesRepository {

    private final Path file;
    private final RecordCodec<BankRules> codec = RecordCodec.of(BankRules.class);

    public PropertiesRulesRepository(Path file) {
        this.file = Objects.requireNonNull(file);
    }

    @Override
    public Optional<BankRules> load() {
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
        Map<String, String> values = properties.stringPropertyNames().stream()
                .collect(Collectors.toMap(Function.identity(), properties::getProperty));
        try {
            return Optional.of(codec.fromStrings(values));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(file + " is invalid: " + e.getMessage(), e);
        }
    }

    /** Writes to a temporary file first, then swaps it in, so a crash never leaves half a file. */
    @Override
    public void save(BankRules rules) {
        Properties properties = new Properties();
        properties.putAll(codec.toStrings(rules));
        try {
            Path folder = file.toAbsolutePath().getParent();
            Files.createDirectories(folder);
            Path temp = Files.createTempFile(folder, "rules", ".tmp");
            try {
                try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Smart Banking rules - change them from the Administration menu");
                }
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }
}