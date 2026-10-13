package com.bank.repository.csv;

import com.bank.exception.AuthenticationException;
import com.bank.model.LoanStatus;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.testsupport.TestBank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

/** The whole bank on CSV files: work with it, "restart" it on the same folder, and check nothing was lost. */
class RestartTest {

    @TempDir
    Path folder;

    @Test
    void customersAccountsTransactionsAndLoansSurviveARestart() {
        TestBank before = TestBank.onDisk(folder).asTeller();
        String ali = before.customer("Ali Khan");
        String savings = before.accounts.openSavings(ali, money("5000")).getAccountNumber();
        before.transactions.withdraw(savings, new BigDecimal("100"));
        String loanId = before.loans.apply(ali, savings, new BigDecimal("2000")).getLoanId();

        TestBank after = TestBank.onDisk(folder).asTeller();

        SavingsAccount account = (SavingsAccount) after.accounts.getByNumber(savings);
        assertAll(
                () -> assertEquals("Ali Khan", after.customers.getById(ali).getFullName()),
                () -> assertEquals(money("4900"), account.getBalance()),
                () -> assertEquals(2, account.remainingWithdrawals(LocalDate.of(2026, 10, 15))),
                () -> assertEquals(2, after.transactions.history(savings).size()),
                () -> assertEquals(LoanStatus.PENDING, after.loans.getById(loanId).getStatus()));
    }

    @Test
    void newIdsContinueWhereTheLastRunStopped() {
        TestBank before = TestBank.onDisk(folder).asTeller();
        String ali = before.customer("Ali Khan");
        String savings = before.accounts.openSavings(ali, money("5000")).getAccountNumber();   // T000001

        TestBank after = TestBank.onDisk(folder).asTeller();
        Transaction deposit = after.transactions.deposit(savings, new BigDecimal("100"));

        assertEquals("T000002", deposit.transactionId());
        assertEquals("C0002", after.customer("Sara Ahmed"));
    }

    @Test
    void aLockedEmployeeIsStillLockedAfterARestart() {
        TestBank before = TestBank.onDisk(folder);
        for (int i = 0; i < 5; i++) {
            assertThrows(AuthenticationException.class,
                    () -> before.auth.login(TestBank.TELLER, "wrong-password".toCharArray()));
        }

        TestBank after = TestBank.onDisk(folder);

        var error = assertThrows(AuthenticationException.class, after::asTeller);
        assertTrue(error.getMessage().contains("locked"));
    }

    @Test
    void theAuditTrailKeepsGrowingAcrossRestarts() {
        TestBank.onDisk(folder).asTeller();
        int afterFirstRun = TestBank.onDisk(folder).asAdmin().employees.auditTrail(1_000).size();

        TestBank third = TestBank.onDisk(folder).asAdmin();

        assertTrue(third.employees.auditTrail(1_000).size() > afterFirstRun);
    }
}
