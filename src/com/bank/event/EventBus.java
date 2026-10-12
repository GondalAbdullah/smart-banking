package com.bank.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Delivers each event to every subscriber, in the order they subscribed.
 * Publishers do not know who is listening, so new reactions can be added without touching them.
 */
public class EventBus {

    private final List<Consumer<BankEvent>> listeners = new ArrayList<>();

    public void subscribe(Consumer<BankEvent> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    /**
     * A failing listener must not undo or block a banking operation that has already happened,
     * so its error is reported and the remaining listeners still run.
     */
    public void publish(BankEvent event) {
        Objects.requireNonNull(event);
        for (Consumer<BankEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                System.err.println("Warning: could not handle " + event.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        }
    }
}