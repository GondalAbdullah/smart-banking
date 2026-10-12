package com.bank.exception;

/** Base type for the bank's own errors, so callers can catch them all in one place. */
public abstract class BankException extends RuntimeException {

    protected BankException(String message) {
        super(message);
    }
}