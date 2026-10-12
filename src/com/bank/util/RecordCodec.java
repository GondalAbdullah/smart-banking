package com.bank.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Turns any record of simple values into named text and back, by reading its components.
 * Add a component to the record and every caller picks it up with no code change.
 * The reflection happens once, in the constructor; after that only method handles are invoked.
 */
public final class RecordCodec<R extends Record> {

    /** How to turn text back into each supported component type. */
    private static final Map<Class<?>, Function<String, Object>> PARSERS = Map.of(
            String.class, text -> text,
            int.class, Integer::valueOf,
            long.class, Long::valueOf,
            BigDecimal.class, BigDecimal::new);

    private final Class<R> type;
    private final List<String> names;
    private final List<Class<?>> types;
    private final List<MethodHandle> accessors;
    private final MethodHandle constructor;

    private RecordCodec(Class<R> type) {
        this.type = type;
        List<String> names = new ArrayList<>();
        List<Class<?>> types = new ArrayList<>();
        List<MethodHandle> accessors = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            for (RecordComponent component : type.getRecordComponents()) {
                if (!PARSERS.containsKey(component.getType())) {
                    throw new IllegalArgumentException(type.getSimpleName() + "." + component.getName()
                            + " has unsupported type " + component.getType().getSimpleName());
                }
                names.add(component.getName());
                types.add(component.getType());
                accessors.add(lookup.unreflect(component.getAccessor()));
            }
            this.constructor = lookup.findConstructor(type, MethodType.methodType(void.class, types));
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot access the components of " + type.getName(), e);
        }
        this.names = List.copyOf(names);
        this.types = List.copyOf(types);
        this.accessors = List.copyOf(accessors);
    }

    public static <R extends Record> RecordCodec<R> of(Class<R> type) {
        return new RecordCodec<>(Objects.requireNonNull(type));
    }

    /** Component names, in declaration order. */
    public List<String> names() {
        return names;
    }

    /** Every component as text, in declaration order. */
    public Map<String, String> toStrings(R record) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            values.put(names.get(i), format(read(i, record)));
        }
        return values;
    }

    /** Builds a record from text. Every component must be present; extra keys are ignored. */
    public R fromStrings(Map<String, String> values) {
        Object[] args = new Object[names.size()];
        for (int i = 0; i < args.length; i++) {
            String text = values.get(names.get(i));
            if (text == null) {
                throw new IllegalArgumentException("Missing value for " + names.get(i));
            }
            args[i] = parse(i, text);
        }
        return construct(args);
    }

    /** A copy of the record with one component changed. The record's own validation still runs. */
    public R with(R record, String name, String text) {
        int index = names.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " field: " + name);
        }
        Object[] args = new Object[names.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = (i == index) ? parse(i, text) : read(i, record);
        }
        return construct(args);
    }

    // ---------- Helpers ----------

    private Object read(int index, R record) {
        try {
            return accessors.get(index).invoke(record);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private R construct(Object[] args) {
        try {
            return type.cast(constructor.invokeWithArguments(args));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private Object parse(int index, String text) {
        Class<?> target = types.get(index);
        try {
            return PARSERS.get(target).apply(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a valid "
                    + target.getSimpleName() + " for " + names.get(index));
        }
    }

    private static String format(Object value) {
        return (value instanceof BigDecimal decimal) ? decimal.toPlainString() : String.valueOf(value);
    }

    /** Method handles throw the target's own exception, unwrapped. Pass unchecked ones straight through. */
    private static RuntimeException rethrow(Throwable t) {
        return switch (t) {
            case RuntimeException e -> e;
            case Error e -> throw e;
            default -> new IllegalStateException(t);
        };
    }
}