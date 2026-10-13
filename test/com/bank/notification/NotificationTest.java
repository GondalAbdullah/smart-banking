package com.bank.notification;

import com.bank.event.BankEvent;
import com.bank.event.EventBus;
import com.bank.exception.TransactionNotAllowedException;
import com.bank.testsupport.TestBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    private TestBank bank;
    private String aliSavings;
    private String saraCurrent;

    @BeforeEach
    void setUp() {
        bank = new TestBank().asTeller();
        aliSavings = bank.accounts.openSavings(bank.customer("Ali Khan"), money("5000")).getAccountNumber();
        saraCurrent = bank.accounts.openCurrent(bank.customer("Sara Ahmed"), money("100")).getAccountNumber();
        bank.outbox.clear();   // opening deposits send nothing anyway; start each test from empty
    }

    @Test
    void depositEmailsTheOwnerWithTheAccountNumberMasked() {
        bank.transactions.deposit(aliSavings, new BigDecimal("500"));

        assertEquals(1, bank.outbox.size());
        EmailMessage email = bank.outbox.getFirst();
        assertAll(
                () -> assertEquals("ali.khan@example.com", email.to()),
                () -> assertEquals("Deposit received", email.subject()),
                () -> assertTrue(email.body().contains("**0001")),
                () -> assertFalse(email.body().contains("A00001")));
    }

    @Test
    void transferEmailsBothSides() {
        bank.transactions.transfer(aliSavings, saraCurrent, new BigDecimal("300"));

        assertEquals(List.of("Transfer sent", "Transfer received"),
                bank.outbox.stream().map(EmailMessage::subject).toList());
        assertEquals("sara.ahmed@example.com", bank.outbox.getLast().to());
    }

    @Test
    void aRefusedWithdrawalSendsNothing() {
        assertThrows(TransactionNotAllowedException.class,
                () -> bank.transactions.withdraw(aliSavings, new BigDecimal("999999")));

        assertTrue(bank.outbox.isEmpty());
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthers() {
        EventBus bus = new EventBus();
        List<BankEvent> received = new ArrayList<>();
        bus.subscribe(event -> { throw new IllegalStateException("broken on purpose"); });
        bus.subscribe(received::add);

        BankEvent event = new BankEvent.AccountClosed("A00001", "C0001", LocalDateTime.now());
        bus.publish(event);

        assertEquals(List.of(event), received);
    }
}