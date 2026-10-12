package com.bank.exception;

/** "Who are you?" failed: unknown user, wrong password, locked account. */
public class AuthenticationException extends BankException {

    public AuthenticationException(String message) {
        super(message);
    }
}