package com.bank.service;

import com.bank.exception.TransactionNotAllowedException;
import com.bank.model.Transaction;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;
import com.bank.testsupport.TestBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TestBank bank;
    private String savings;   // 5,000, minimum balance 1,000, 3 withdrawals a month
    private String current;   // 1,000, overdraft 10,000
    private String fixed;     // 8,000, 12 months

    @BeforeEach
    void setUp() {
        bank = new TestBank().asTeller();
        String ali = bank.customer("Ali Khan");
        savings = bank.accounts.openSavings(ali, money("5000")).getAccountNumber();
        current = bank.accounts.openCurrent(ali, money("1000")).getAccountNumber();
        fixed = bank.accounts.openFixedDeposit(ali, money("8000"), 12).getAccountNumber();
    }

    private BigDecimal balanceOf(String accountNumber) {
        return bank.accounts.getByNumber(accountNumber).getBalance();
    }

    @Test
    void depositAddsMoneyAndRecordsATransaction() {
        Transaction transaction = bank.transactions.deposit(savings, new BigDecimal("500"));

        assertEquals(money("5500"), balanceOf(savings));
        assertEquals(TransactionType.DEPOSIT, transaction.type());
        assertEquals(TestBank.TELLER, transaction.performedBy());
    }

    @Test
    void transferMovesMoneyBetweenAccounts() {
        bank.transactions.transfer(savings, current, new BigDecimal("1500"));

        assertEquals(money("3500"), balanceOf(savings));
        assertEquals(money("2500"), balanceOf(current));
    }

    @Test
    void refusedTransferChangesNeitherBalance() {
        assertThrows(TransactionNotAllowedException.class,
                () -> bank.transactions.transfer(savings, fixed, new BigDecimal("1000")));

        assertEquals(money("5000"), balanceOf(savings));
        assertEquals(money("8000"), balanceOf(fixed));
    }

    @Test
    void refusedTransferIsRecordedAsFailed() {
        assertThrows(TransactionNotAllowedException.class,
                () -> bank.transactions.transfer(savings, fixed, new BigDecimal("1000")));

        Transaction last = bank.transactions.history(savings).getLast();
        assertEquals(TransactionType.TRANSFER, last.type());
        assertEquals(TransactionStatus.FAILED, last.status());
    }

    @Test
    void badInputIsRejectedWithoutBeingRecorded() {
        int before = bank.transactions.history(savings).size();

        assertThrows(IllegalArgumentException.class,
                () -> bank.transactions.withdraw(savings, new BigDecimal("-5")));

        assertEquals(before, bank.transactions.history(savings).size());
    }

    @Test
    void savingsWithdrawalLimitResetsOnTheFirstOfTheMonth() {
        bank.clock.set(LocalDateTime.of(2026, 10, 31, 23, 0));
        for (int i = 0; i < 3; i++) {
            bank.transactions.withdraw(savings, new BigDecimal("100"));
        }
        assertThrows(TransactionNotAllowedException.class,
                () -> bank.transactions.withdraw(savings, new BigDecimal("100")));

        bank.clock.advanceDays(1);   // 1 November
        bank.transactions.withdraw(savings, new BigDecimal("100"));

        assertEquals(money("4600"), balanceOf(savings));
    }

    @Test
    void miniStatementShowsTheLastFiveSuccessfulTransactionsNewestFirst() {
        for (int i = 1; i <= 6; i++) {
            bank.transactions.deposit(current, new BigDecimal(i));
        }
        assertThrows(TransactionNotAllowedException.class,
                () -> bank.transactions.withdraw(current, new BigDecimal("999999")));

        List<Transaction> statement = bank.transactions.miniStatement(current);

        assertEquals(5, statement.size());
        assertEquals(money("6"), statement.getFirst().amount());
        assertTrue(statement.stream().allMatch(Transaction::isSuccessful));
    }

    @Test
    void searchFiltersByType() {
        bank.transactions.deposit(savings, new BigDecimal("100"));
        bank.transactions.withdraw(savings, new BigDecimal("50"));

        List<Transaction> deposits = bank.transactions.search(savings, null, null, TransactionType.DEPOSIT);

        assertEquals(1, deposits.size());
        assertEquals(money("100"), deposits.getFirst().amount());
    }
}