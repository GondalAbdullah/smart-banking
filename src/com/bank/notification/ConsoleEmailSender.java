package com.bank.notification;

/** Simulated email, as the specification asks: the message is printed instead of sent. */
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        System.out.print("""

                ----------- EMAIL (simulated) -----------
                To:      %s
                Subject: %s

                %s
                -----------------------------------------
                """.formatted(message.to(), message.subject(), message.body()));
    }
}