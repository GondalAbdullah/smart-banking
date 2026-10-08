package com.bank;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== Smart Banking System ===");
            System.out.println("1. Customers");
            System.out.println("2. Accounts");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> System.out.println("Customer menu (coming soon)");
                case "2" -> System.out.println("Account menu (coming soon)");
                case "0" -> running = false;
                default -> System.out.println("Invalid option, try again.");
            }
        }
        System.out.println("Goodbye.");
        scanner.close();
    }
}