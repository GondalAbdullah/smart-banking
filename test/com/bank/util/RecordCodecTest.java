package com.bank.util;

import com.bank.config.BankRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordCodecTest {

    private final RecordCodec<BankRules> codec = RecordCodec.of(BankRules.class);
    private final BankRules defaults = BankRules.defaults();

    @Test
    void namesFollowTheRecordsDeclarationOrder() {
        assertEquals(List.of("savingsMinimumBalance", "savingsMonthlyWithdrawalLimit",
                        "savingsAnnualInterestRate", "currentOverdraftLimit",
                        "fixedDepositAnnualInterestRate"),
                codec.names());
    }

    @Test
    void savingAndLoadingGivesBackAnEqualRecord() {
        Map<String, String> text = codec.toStrings(defaults);

        assertEquals(defaults, codec.fromStrings(text));
    }

    @Test
    void withChangesOnlyTheNamedValue() {
        BankRules changed = codec.with(defaults, "savingsMonthlyWithdrawalLimit", "5");

        assertEquals(5, changed.savingsMonthlyWithdrawalLimit());
        assertEquals(defaults.savingsMinimumBalance(), changed.savingsMinimumBalance());
        assertEquals(defaults.currentOverdraftLimit(), changed.currentOverdraftLimit());
    }

    @Test
    void rejectsAnUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> codec.with(defaults, "noSuchRule", "1"));
    }

    @Test
    void rejectsTextOfTheWrongType() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> codec.with(defaults, "savingsMonthlyWithdrawalLimit", "3.5"));

        assertTrue(error.getMessage().contains("not a valid int"));
    }

    @Test
    void theRecordsOwnValidationStillRuns() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> codec.with(defaults, "savingsAnnualInterestRate", "5"));

        assertTrue(error.getMessage().contains("fraction between 0 and 1"));
    }

    @Test
    void rejectsAMissingValue() {
        assertThrows(IllegalArgumentException.class,
                () -> codec.fromStrings(Map.of("savingsMinimumBalance", "1000")));
    }
}