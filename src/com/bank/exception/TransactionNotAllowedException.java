package com.bank.exception;

public class TransactionNotAllowedException extends IllegalStateException {

    public TransactionNotAllowedException(String message) {
        super(message);
    }
}