package com.bank.ui;

import com.bank.exception.NotFoundException;
import com.bank.model.Customer;
import com.bank.service.CustomerService;
import com.bank.service.CustomerUpdate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerMenu {

    private static final String ROW_FORMAT = "%-6s %-22s %-26s %-13s %-9s%n";

    private final CustomerService customerService;
    private final ConsoleInput input;
    private final Map<String, MenuOption> options = new LinkedHashMap<>();

    public CustomerMenu(CustomerService customerService, ConsoleInput input) {
        this.customerService = customerService;
        this.input = input;

        options.put("1", new MenuOption("Register new customer", this::registerCustomer));
        options.put("2", new MenuOption("View customer details (by ID)", this::viewCustomer));
        options.put("3", new MenuOption("Search customers by name", this::searchByName));
        options.put("4", new MenuOption("List all customers", this::listAllCustomers));
        options.put("5", new MenuOption("Update customer information", this::updateCustomer));
        options.put("6", new MenuOption("Deactivate customer", this::deactivateCustomer));
    }

    public void show() {
        while (true) {
            System.out.println("\n--- Customer Management ---");
            options.forEach((key, option) -> System.out.println(key + ". " + option.label()));
            System.out.println("0. Back");

            String choice = input.readLine("Choose: ");
            if (choice.equals("0")) {
                return;
            }

            MenuOption option = options.get(choice);
            if (option == null) {
                System.out.println("Invalid option, try again.");
                continue;
            }
            runSafely(option.action());
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ---------- Actions ----------

    private void registerCustomer() {
        String fullName = input.readRequired("Full name: ");
        String email = input.readRequired("Email: ");
        String phone = input.readRequired("Phone: ");
        String address = input.readRequired("Address: ");

        Customer customer = customerService.register(fullName, email, phone, address);
        System.out.println("Customer registered with ID " + customer.getCustomerId());
    }

    private void viewCustomer() {
        Customer customer = customerService.getById(input.readRequired("Customer ID: "));
        printDetails(customer);
    }

    private void searchByName() {
        String fragment = input.readRequired("Name contains: ");
        printTable(customerService.searchByName(fragment));
    }

    private void listAllCustomers() {
        printTable(customerService.listAll());
    }

    private void updateCustomer() {
        Customer current = customerService.getById(input.readRequired("Customer ID: "));
        System.out.println("Press Enter to keep the current value.");

        CustomerUpdate changes = new CustomerUpdate(
                input.readLine("Full name [" + current.getFullName() + "]: "),
                input.readLine("Email [" + current.getEmail() + "]: "),
                input.readLine("Phone [" + current.getPhone() + "]: "),
                input.readLine("Address [" + current.getAddress() + "]: "));

        Customer updated = customerService.updateDetails(current.getCustomerId(), changes);
        System.out.println("Customer updated.");
        printDetails(updated);
    }

    private void deactivateCustomer() {
        Customer customer = customerService.getById(input.readRequired("Customer ID: "));
        printDetails(customer);

        if (!input.confirm("Deactivate this customer?")) {
            System.out.println("Cancelled.");
            return;
        }
        customerService.deactivate(customer.getCustomerId());
        System.out.println("Customer " + customer.getCustomerId() + " deactivated.");
    }

    // ---------- Output helpers ----------

    private void printDetails(Customer c) {
        System.out.print("""
                ID:          %s
                Name:        %s
                Email:       %s
                Phone:       %s
                Address:     %s
                Registered:  %s
                Status:      %s
                """.formatted(c.getCustomerId(), c.getFullName(), c.getEmail(),
                c.getPhone(), c.getAddress(), c.getRegistrationDate(), c.getStatus()));
    }

    private void printTable(List<Customer> customers) {
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        System.out.printf(ROW_FORMAT, "ID", "Name", "Email", "Phone", "Status");
        customers.forEach(c -> System.out.printf(ROW_FORMAT,
                c.getCustomerId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getStatus()));
        System.out.println(customers.size() + " customer(s)");
    }
}