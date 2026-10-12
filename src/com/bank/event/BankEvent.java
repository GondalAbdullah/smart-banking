package com.bank.event;

import com.bank.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Something that has already happened in the bank. Events are facts in the past tense,
 * published after the change is complete. Listeners react to them; they cannot undo them.
 */
public sealed interface BankEvent {

    record DepositMade(Transaction transaction, String customerId, BigDecimal balanceAfter)
            implements BankEvent {
    }

    record WithdrawalMade(Transaction transaction, String customerId, BigDecimal balanceAfter)
            implements BankEvent {
    }

    record TransferMade(Transaction transaction,
                        String senderId, BigDecimal senderBalanceAfter,
                        String recipientId, BigDecimal recipientBalanceAfter)
            implements BankEvent {
    }

    record AccountClosed(String accountNumber, String customerId, LocalDateTime closedAt)
            implements BankEvent {
    }

    record LoanApproved(String loanId, String customerId, String accountNumber, BigDecimal principal)
            implements BankEvent {
    }

    record LoanRejected(String loanId, String customerId, BigDecimal principal)
            implements BankEvent {
    }
}