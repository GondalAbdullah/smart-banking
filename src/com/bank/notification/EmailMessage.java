package com.bank.notification;

/** One email: who it goes to, the subject line and the text. */
public record EmailMessage(String to, String subject, String body) {

    public EmailMessage {
        to = requireNonBlank(to, "Recipient");
        subject = requireNonBlank(subject, "Subject");
        body = requireNonBlank(body, "Body");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}