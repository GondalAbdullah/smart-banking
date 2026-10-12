package com.bank.exception;

/** "May you do this?" failed: the employee is known, but their role does not allow the action. */
public class AccessDeniedException extends BankException {

    public AccessDeniedException(String message) {
        super(message);
    }
}