package com.bank.notification;

/** Anything that can deliver an email. Swap the console version for a real one without touching callers. */
public interface EmailSender {

    void send(EmailMessage message);
}