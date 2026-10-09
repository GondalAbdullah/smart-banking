package com.bank.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Customer {
 
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?\\d{10,15}$");
    
    private final String customerId;
    private final LocalDate registrationDate;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private CustomerStatus status;

    private Customer(String customerId, String fullName, String email, String phone,
                     String address, LocalDate registrationDate, CustomerStatus status) {
        this.customerId = requireNonBlank(customerId, "Customer ID");
        this.fullName = requireNonBlank(fullName, "Full name");
        this.email = validateEmail(email);
        this.phone = validatePhone(phone);
        this.address = requireNonBlank(address, "Address");
        this.registrationDate = Objects.requireNonNull(registrationDate, "Registration date is required");
        this.status = Objects.requireNonNull(status, "Status is required");
    }

    /** Creates a brand-new customer: registered today, active. */
    public static Customer register(String customerId, String fullName, String email,
                                    String phone, String address) {
        return new Customer(customerId, fullName, email, phone, address,
                LocalDate.now(), CustomerStatus.ACTIVE);
    }

    /** Rebuilds an existing customer from storage, keeping its original date and status. */
    public static Customer restore(String customerId, String fullName, String email,
                                   String phone, String address,
                                   LocalDate registrationDate, CustomerStatus status) {
        return new Customer(customerId, fullName, email, phone, address,
                registrationDate, status);
    }

    // ---------- Getters ----------

    public String getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public CustomerStatus getStatus() { return status; }

    // ---------- Validated setters ----------

    public void setFullName(String fullName) {
        this.fullName = requireNonBlank(fullName, "Full name");
    }

    public void setEmail(String email) {
        this.email = validateEmail(email);
    }

    public void setPhone(String phone) {
        this.phone = validatePhone(phone);
    }

    public void setAddress(String address) {
        this.address = requireNonBlank(address, "Address");
    }

    // ---------- Behaviour ----------

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public void deactivate() {
        if (!isActive()) {
            throw new IllegalStateException("Customer " + customerId + " is already inactive");
        }
        status = CustomerStatus.INACTIVE;
    }

    // ---------- Validation helpers ----------

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String validateEmail(String email) {
        String normalized = requireNonBlank(email, "Email").toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        return normalized;
    }

    private static String validatePhone(String phone) {
        String normalized = requireNonBlank(phone, "Phone").replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid phone number: " + phone);
        }
        return normalized;
    }

    // ---------- Identity ----------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return customerId.equals(other.customerId);
    }

    @Override
    public int hashCode() {
        return customerId.hashCode();
    }

    @Override
    public String toString() {
        return "%s | %s | %s | %s | %s | registered %s | %s".formatted(
                customerId, fullName, email, phone, address, registrationDate, status);
    }
    
}
