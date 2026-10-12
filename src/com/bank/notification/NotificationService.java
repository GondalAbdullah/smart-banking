package com.bank.notification;

import com.bank.event.BankEvent;
import com.bank.exception.NotFoundException;
import com.bank.model.Customer;
import com.bank.repository.CustomerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Listens for bank events and emails the customers involved. */
public class NotificationService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerRepository customers;
    private final EmailSender sender;

    public NotificationService(CustomerRepository customers, EmailSender sender) {
        this.customers = Objects.requireNonNull(customers);
        this.sender = Objects.requireNonNull(sender);
    }

    /** Subscribed to the EventBus as notificationService::handle. */
    public void handle(BankEvent event) {
        switch (event) {
            case BankEvent.DepositMade(var tx, var customerId, var balance) ->
                    send(customerId, "Deposit received",
                            "A deposit of " + money(tx.amount()) + " was made to account "
                                    + masked(tx.destinationAccount()) + " on " + when(tx.timestamp()) + ".",
                            "Reference: " + tx.transactionId(),
                            "New balance: " + money(balance));

            case BankEvent.WithdrawalMade(var tx, var customerId, var balance) ->
                    send(customerId, "Withdrawal made",
                            "A withdrawal of " + money(tx.amount()) + " was made from account "
                                    + masked(tx.sourceAccount()) + " on " + when(tx.timestamp()) + ".",
                            "Reference: " + tx.transactionId(),
                            "New balance: " + money(balance));

            case BankEvent.TransferMade(var tx, var senderId, var senderBalance,
                                        var recipientId, var recipientBalance) -> {
                send(senderId, "Transfer sent",
                        "You transferred " + money(tx.amount()) + " from account "
                                + masked(tx.sourceAccount()) + " to account "
                                + masked(tx.destinationAccount()) + " on " + when(tx.timestamp()) + ".",
                        "Reference: " + tx.transactionId(),
                        "New balance: " + money(senderBalance));
                send(recipientId, "Transfer received",
                        "You received " + money(tx.amount()) + " in account "
                                + masked(tx.destinationAccount()) + " on " + when(tx.timestamp()) + ".",
                        "Reference: " + tx.transactionId(),
                        "New balance: " + money(recipientBalance));
            }

            case BankEvent.AccountClosed(var accountNumber, var customerId, var closedAt) ->
                    send(customerId, "Account closed",
                            "Your account " + masked(accountNumber) + " was closed on " + when(closedAt) + ".",
                            "If you did not request this, please contact your branch immediately.");

            case BankEvent.LoanApproved(var loanId, var customerId, var accountNumber, var principal) ->
                    send(customerId, "Loan approved",
                            "Your loan " + loanId + " of " + money(principal) + " has been approved.",
                            "The money has been credited to account " + masked(accountNumber) + ".");

            case BankEvent.LoanRejected(var loanId, var customerId, var principal) ->
                    send(customerId, "Loan application not approved",
                            "Your loan application " + loanId + " for " + money(principal) + " was not approved.",
                            "Please contact your branch for details.");
        }
    }

    private void send(String customerId, String subject, String... lines) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new NotFoundException("No customer found with ID: " + customerId));
        String body = "Dear " + customer.getFullName() + ",\n\n"
                + String.join("\n", lines)
                + "\n\nSmart Banking";
        sender.send(new EmailMessage(customer.getEmail(), subject, body));
    }

    /** Emails are not a secure channel, so only the last 4 characters of an account number are shown. */
    private static String masked(String accountNumber) {
        int visible = Math.min(4, accountNumber.length());
        return "*".repeat(accountNumber.length() - visible)
                + accountNumber.substring(accountNumber.length() - visible);
    }

    private static String money(BigDecimal amount) {
        return "%,.2f".formatted(amount);
    }

    private static String when(LocalDateTime time) {
        return WHEN.format(time);
    }
}