package com.bank.service;

import com.bank.exception.AccessDeniedException;
import com.bank.exception.AuthenticationException;
import com.bank.model.AuditAction;
import com.bank.model.Role;
import com.bank.testsupport.TestBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

/** Calls services directly, with no menus involved: the service layer must enforce every rule on its own. */
class PermissionTest {

    private TestBank bank;
    private String account;   // a savings account opened by the teller

    @BeforeEach
    void setUp() {
        bank = new TestBank().asTeller();
        account = bank.accounts.openSavings(bank.customer("Ali Khan"), money("5000")).getAccountNumber();
    }

    @Test
    void nobodyLoggedInCannotDoAnything() {
        bank.auth.logout();

        assertThrows(AuthenticationException.class, () -> bank.customer("Sara Ahmed"));
    }

    @Nested
    @DisplayName("Teller")
    class Teller {

        @Test
        void canProcessTransactions() {
            bank.transactions.deposit(account, new BigDecimal("100"));

            assertEquals(money("5100"), bank.accounts.getByNumber(account).getBalance());
        }

        @Test
        void cannotFreezeAccounts() {
            assertThrows(AccessDeniedException.class, () -> bank.accounts.freeze(account));
        }

        @Test
        void cannotApplyInterest() {
            assertThrows(AccessDeniedException.class, bank.transactions::applyMonthlyInterest);
        }

        @Test
        void cannotApproveLoans() {
            String loanId = bank.loans.apply("C0001", account, new BigDecimal("1000")).getLoanId();

            assertThrows(AccessDeniedException.class, () -> bank.loans.approve(loanId));
        }

        @Test
        void cannotSeeTheBranchReport() {
            assertThrows(AccessDeniedException.class, bank.reports::branchReport);
        }

        @Test
        void cannotManageEmployeesOrRules() {
            assertAll(
                    () -> assertThrows(AccessDeniedException.class,
                            () -> bank.employees.hire("X", Role.ADMIN, "Temporary1".toCharArray())),
                    () -> assertThrows(AccessDeniedException.class,
                            () -> bank.rules.update("savingsMinimumBalance", "1")));
        }
    }

    @Nested
    @DisplayName("Manager")
    class Manager {

        @BeforeEach
        void login() {
            bank.asManager();
        }

        @Test
        void canFreezeAccountsAndApplyInterest() {
            bank.transactions.applyMonthlyInterest();
            bank.accounts.freeze(account);

            assertEquals(money("5016.67"), bank.accounts.getByNumber(account).getBalance());
        }

        @Test
        void cannotHireEmployees() {
            assertThrows(AccessDeniedException.class,
                    () -> bank.employees.hire("X", Role.TELLER, "Temporary1".toCharArray()));
        }
    }

    @Nested
    @DisplayName("Admin")
    class Admin {

        @BeforeEach
        void login() {
            bank.asAdmin();
        }

        @Test
        void cannotMoveMoney() {
            assertThrows(AccessDeniedException.class,
                    () -> bank.transactions.deposit(account, new BigDecimal("100")));
        }

        @Test
        void cannotChangeTheirOwnRole() {
            assertThrows(IllegalArgumentException.class,
                    () -> bank.employees.changeRole(TestBank.ADMIN, Role.TELLER));
        }
    }

    @Test
    void refusalsAreRecordedInTheAuditTrail() {
        assertThrows(AccessDeniedException.class, bank.transactions::applyMonthlyInterest);

        bank.asAdmin();
        assertTrue(bank.employees.auditTrail(50).stream().anyMatch(entry ->
                entry.action() == AuditAction.ACCESS_DENIED && entry.details().equals("APPLY_INTEREST")));
    }

    @Test
    void temporaryPasswordMustBeChangedBeforeAnythingElse() {
        bank.asAdmin();
        bank.employees.hire("New Teller", Role.TELLER, "Temporary1".toCharArray());   // E0004
        bank.auth.logout();
        bank.auth.login("E0004", "Temporary1".toCharArray());

        var error = assertThrows(AccessDeniedException.class, () -> bank.customer("Sara Ahmed"));
        assertTrue(error.getMessage().contains("temporary password"));
    }

    @Test
    void fiveWrongPasswordsLockTheAccountUntilAnAdminResetsIt() {
        bank.auth.logout();
        for (int i = 0; i < 5; i++) {
            assertThrows(AuthenticationException.class,
                    () -> bank.auth.login(TestBank.TELLER, "wrong-password".toCharArray()));
        }

        var error = assertThrows(AuthenticationException.class,
                () -> bank.auth.login(TestBank.TELLER, TestBank.password()));
        assertTrue(error.getMessage().contains("locked"));

        bank.asAdmin();
        bank.employees.resetPassword(TestBank.TELLER, "Temporary2".toCharArray());
        bank.auth.logout();
        assertEquals(TestBank.TELLER,
                bank.auth.login(TestBank.TELLER, "Temporary2".toCharArray()).getEmployeeId());
    }
}