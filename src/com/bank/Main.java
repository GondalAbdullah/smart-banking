package com.bank;

import com.bank.repository.CustomerRepository;
import com.bank.repository.InMemoryCustomerRepository;
import com.bank.service.CustomerService;
import com.bank.ui.ConsoleInput;
import com.bank.ui.CustomerMenu;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput(new Scanner(System.in));

        CustomerRepository customerRepository = new InMemoryCustomerRepository();
        CustomerService customerService = new CustomerService(customerRepository);
        CustomerMenu customerMenu = new CustomerMenu(customerService, input);

        while (true) {
            System.out.println("\n=== Smart Banking System ===");
            System.out.println("1. Customers");
            System.out.println("0. Exit");

            switch (input.readLine("Choose: ")) {
                case "1" -> customerMenu.show();
                case "0" -> {
                    System.out.println("Goodbye.");
                    return;
                }
                default -> System.out.println("Invalid option, try again.");
            }
        }
    }
}