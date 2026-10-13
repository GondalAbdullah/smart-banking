package com.bank.model;

import com.bank.exception.TransactionNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private static final LocalDate OCT_15 = LocalDate.of(2026, 10, 15);

    private static SavingsAccount savings(String deposit) {
        return SavingsAccount.open("A00001", "C0001", new BigDecimal(deposit),
                new BigDecimal("1000"), new BigDecimal("0.04"), 3, OCT_15);
    }

    @Nested
    @DisplayName("Savings account")
    class Savings {

        @Test
        void cannotOpenBelowTheMinimumBalance() {
            assertThrows(TransactionNotAllowedException.class, () -> savings("999"));
        }

        @Test
        void cannotWithdrawBelowTheMinimumBalance() {
            SavingsAccount account = savings("5000");

            var error = assertThrows(TransactionNotAllowedException.class,
                    () -> account.withdraw(new BigDecimal("4500"), OCT_15));

            assertTrue(error.getMessage().contains("at most 4000.00"));
            assertEquals(money("5000"), account.getBalance());
        }

        @Test
        void fourthWithdrawalInTheSameMonthIsRefused() {
            SavingsAccount account = savings("5000");
            for (int i = 0; i < 3; i++) {
                account.withdraw(new BigDecimal("100"), OCT_15);
            }

            assertThrows(TransactionNotAllowedException.class,
                    () -> account.withdraw(new BigDecimal("100"), OCT_15));
        }

        @Test
        void withdrawalLimitResetsInANewMonth() {
            SavingsAccount account = savings("5000");
            LocalDate oct31 = LocalDate.of(2026, 10, 31);
            for (int i = 0; i < 3; i++) {
                account.withdraw(new BigDecimal("100"), oct31);
            }

            account.withdraw(new BigDecimal("100"), oct31.plusDays(1));   // 1 November

            assertEquals(money("4600"), account.getBalance());
            assertEquals(2, account.remainingWithdrawals(oct31.plusDays(1)));
        }

        @Test
        void monthlyInterestIsBalanceTimesRateDividedByTwelve() {
            assertEquals(money("20"), savings("6000").calculateMonthlyInterest());   // 6000 x 4% / 12
        }
    }

    @Nested
    @DisplayName("Current account")
    class Current {

        private final CurrentAccount account = CurrentAccount.open(
                "A00002", "C0001", new BigDecimal("100"), new BigDecimal("10000"), OCT_15);

        @Test
        void canGoNegativeUpToTheOverdraftLimit() {
            account.withdraw(new BigDecimal("10100"), OCT_15);

            assertEquals(money("-10000"), account.getBalance());
            assertThrows(TransactionNotAllowedException.class,
                    () -> account.withdraw(new BigDecimal("0.01"), OCT_15));
        }

        @Test
        void earnsNoInterest() {
            assertEquals(BigDecimal.ZERO, account.calculateMonthlyInterest());
        }
    }

    @Nested
    @DisplayName("Fixed deposit")
    class FixedDeposit {

        private final FixedDepositAccount account = FixedDepositAccount.open(
                "A00003", "C0001", new BigDecimal("8000"), new BigDecimal("0.07"), 12, OCT_15);

        @Test
        void refusesDepositsAfterOpening() {
            assertThrows(TransactionNotAllowedException.class,
                    () -> account.deposit(new BigDecimal("100")));
        }

        @Test
        void refusesWithdrawalsBeforeMaturity() {
            assertThrows(TransactionNotAllowedException.class,
                    () -> account.withdraw(new BigDecimal("100"), OCT_15.plusMonths(11)));
        }

        @Test
        void allowsWithdrawalOnTheMaturityDate() {
            account.withdraw(new BigDecimal("8000"), OCT_15.plusMonths(12));

            assertEquals(money("0"), account.getBalance());
        }
    }

    @Nested
    @DisplayName("Rules shared by every account")
    class Shared {

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1", "0.001"})
        void rejectsInvalidAmounts(BigDecimal amount) {
            SavingsAccount account = savings("5000");

            assertThrows(IllegalArgumentException.class, () -> account.deposit(amount));
        }

        @Test
        void checkingAWithdrawalChangesNothing() {
            SavingsAccount account = savings("5000");

            account.checkWithdrawal(new BigDecimal("1000"), OCT_15);

            assertEquals(money("5000"), account.getBalance());
            assertEquals(3, account.remainingWithdrawals(OCT_15));
        }

        @Test
        void frozenAccountRefusesTransactions() {
            SavingsAccount account = savings("5000");
            account.freeze();

            assertThrows(TransactionNotAllowedException.class,
                    () -> account.deposit(new BigDecimal("100")));
        }

        @Test
        void canOnlyBeClosedWithAZeroBalance() {
            SavingsAccount account = savings("5000");

            assertThrows(IllegalStateException.class, account::close);
        }
    }
}