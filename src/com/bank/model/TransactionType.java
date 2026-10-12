package com.bank.model;

/**
 * What kind of money movement a transaction is, and which sides it has.
 * Money flows from the source to the destination; a missing side means cash
 * (or the bank itself) on that end.
 */
public enum TransactionType {

    OPENING_DEPOSIT(false, true),
    DEPOSIT(false, true),
    WITHDRAWAL(true, false),
    TRANSFER(true, true),
    INTEREST(false, true);

    private final boolean hasSource;
    private final boolean hasDestination;

    TransactionType(boolean hasSource, boolean hasDestination) {
        this.hasSource = hasSource;
        this.hasDestination = hasDestination;
    }

    /** Package-private: only Transaction's constructor needs this check. */
    void checkSides(String source, String destination) {
        if (hasSource != (source != null)) {
            throw new IllegalArgumentException(
                    this + (hasSource ? " needs" : " cannot have") + " a source account");
        }
        if (hasDestination != (destination != null)) {
            throw new IllegalArgumentException(
                    this + (hasDestination ? " needs" : " cannot have") + " a destination account");
        }
    }
}