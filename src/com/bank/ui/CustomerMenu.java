package com.bank.ui;

import com.bank.model.Customer;
import com.bank.service.AccountService;
import com.bank.service.CustomerService;
import com.bank.service.CustomerUpdate;

import java.util.List;

public class CustomerMenu {

    private static final String ROW_FORMAT = "%-6s %-22s %-26s %-13s %-9s%n";

    private final CustomerService customerService;
    private final AccountService accountService;
    private final ConsoleInput input;
    private final Menu menu;

    public CustomerMenu(CustomerService customerService, AccountService accountService,
                        ConsoleInput input) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.input = input;
        this.menu = new Menu("Customer Management", input)
                .option("Register new customer", this::registerCustomer)
                .option("View customer details (by ID)", this::viewCustomer)
                .option("Search customers by name", this::searchByName)
                .option("List all customers", this::listAllCustomers)
                .option("Update customer information", this::updateCustomer)
                .option("Deactivate customer", this::deactivateCustomer)
                .option("View customer's accounts", this::viewCustomerAccounts);
    }

    public void show() {
        menu.show();
    }

    // ---------- Actions ----------

    private void viewCustomerAccounts() {
        Customer customer = customerService.getById(input.readRequired("Customer ID: "));
        System.out.println("Accounts of " + customer.getFullName()
                + " (" + customer.getCustomerId() + ")");
        AccountPrinter.printTable(accountService.findByCustomer(customer.getCustomerId()));
    }

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