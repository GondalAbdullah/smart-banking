package com.bank.ui;

import com.bank.exception.NotFoundException;
import com.bank.model.Permission;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/** Drives a real Menu with typed input supplied as a string, instead of a person at the keyboard. */
class MenuTest {

    private final List<String> ran = new ArrayList<>();

    private static ConsoleInput typing(String... lines) {
        return new ConsoleInput(new Scanner(String.join("\n", lines) + "\n"));
    }

    @Test
    void aBankErrorIsShownAndTheMenuKeepsRunning() {
        Menu menu = new Menu("Test", typing("1", "1", "0"), permission -> true)
                .option("Look up a customer", () -> {
                    ran.add("lookup");
                    throw new NotFoundException("No customer found with ID: C9999");
                });

        menu.show();   // must not crash

        assertEquals(2, ran.size());   // the user could choose the option again after the error
    }

    @Test
    void optionsWithoutPermissionAreHiddenAndCannotBeChosen() {
        Menu menu = new Menu("Test", typing("1", "2", "0"), permission -> false)
                .option("Everyone", () -> ran.add("everyone"))
                .option("Managers only", Permission.APPROVE_LOANS, () -> ran.add("managers"));

        menu.show();   // "2" does not exist for this user, so it is an invalid choice

        assertEquals(List.of("everyone"), ran);
    }
}