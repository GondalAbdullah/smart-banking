package com.bank.service;

import com.bank.config.BankRules;
import com.bank.exception.TransactionNotAllowedException;
import com.bank.model.SavingsAccount;
import com.bank.repository.PropertiesRulesRepository;
import com.bank.testsupport.TestBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

/** Uses a real rules.properties file, in a fresh temporary folder that JUnit deletes afterwards. */
class RulesServiceTest {

    @TempDir
    Path folder;

    private Path file;
    private TestBank bank;

    @BeforeEach
    void setUp() {
        file = folder.resolve("rules.properties");
        bank = new TestBank(new PropertiesRulesRepository(file));
    }

    @Test
    void firstRunSavesTheDefaults() {
        assertTrue(Files.exists(file));
        assertEquals(BankRules.defaults(), new PropertiesRulesRepository(file).load().orElseThrow());
    }

    @Test
    void aChangeSurvivesARestart() {
        bank.asAdmin();

        bank.rules.update("currentOverdraftLimit", "25000");

        BankRules reloaded = new PropertiesRulesRepository(file).load().orElseThrow();
        assertEquals(new BigDecimal("25000"), reloaded.currentOverdraftLimit());
    }

    @Test
    void aRejectedValueLeavesTheFileUntouched() throws IOException {
        bank.asAdmin();
        String before = Files.readString(file);

        assertThrows(IllegalArgumentException.class,
                () -> bank.rules.update("savingsMonthlyWithdrawalLimit", "0"));

        assertEquals(before, Files.readString(file));
    }

    @Test
    void aCorruptFileStopsStartupWithAClearMessage() throws IOException {
        Files.writeString(file, "savingsMinimumBalance=abc\n");

        var error = assertThrows(IllegalStateException.class,
                () -> new PropertiesRulesRepository(file).load());
        assertTrue(error.getMessage().contains("'abc' is not a valid BigDecimal"));
    }

    @Test
    void newRulesApplyOnlyToAccountsOpenedAfterwards() {
        bank.asTeller();
        String ali = bank.customer("Ali Khan");
        SavingsAccount older = bank.accounts.openSavings(ali, money("1200"));

        bank.asAdmin();
        bank.rules.update("savingsMinimumBalance", "1500");
        bank.asTeller();

        assertEquals(new BigDecimal("1000"), older.getMinimumBalance());
        assertThrows(TransactionNotAllowedException.class,
                () -> bank.accounts.openSavings(ali, money("1200")));
    }
}