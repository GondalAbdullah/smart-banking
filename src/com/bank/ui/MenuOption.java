package com.bank.ui;

import com.bank.model.Permission;

import java.util.function.Predicate;

/** A menu line. A null permission means every logged-in employee may use it. */
record MenuOption(String label, Permission required, Runnable action) {

    boolean isAllowed(Predicate<Permission> allowed) {
        return required == null || allowed.test(required);
    }
}