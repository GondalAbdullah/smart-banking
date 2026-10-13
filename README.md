# Smart Banking Management System

A console banking application written in Core Java 21. Bank staff log in to manage customers, accounts, transactions and loans. What each person can do depends on their role.

## Features

- **Customers:** register, update, deactivate and search.
- **Accounts:** Savings, Current and Fixed Deposit accounts, each with its own rules. You can freeze and close accounts.
- **Transactions:** deposit, withdraw, transfer, apply monthly interest, view history, search, and print a mini statement.
- **Loans:** a teller records an application, a manager approves or rejects it, and repayments are taken from the linked account.
- **Staff and security:** employee login, Teller / Manager / Admin roles, salted password hashing, lockout after 5 wrong passwords, and an audit trail.
- **Configurable rules:** the minimum balance, overdraft limit, interest rates and withdrawal limit are stored in `data/rules.properties`.
- **Email notifications:** printed to the console.
- **Branch report:** customers, accounts, deposits held and loans.

## Requirements

- JDK 21 or newer
- Maven 3.8 or newer

Check both with `java -version` and `mvn -v`. Maven must report Java 21 or newer, otherwise the build fails on the `switch` pattern matching the code uses.

JUnit 5 is the only library, and it is used only for tests. Maven downloads it the first time you build, so the first build needs an internet connection.

## Run the app

Run these commands from the project folder. The app reads and writes `data/rules.properties` relative to the folder you start it from.

```bash
mvn package
java -jar target/smart-banking.jar
```

`mvn package` runs the tests before building the jar. To build without them, use `mvn package -DskipTests`.

Run the app in a real terminal (including the VS Code terminal) so that passwords are hidden while you type.

### First login

On the first run, the app creates an administrator account:

| Employee ID | Password   |
|-------------|------------|
| `E0001`     | `admin123` |

You must change this password the first time you log in. After that, add tellers and managers from **Administration → Hire employee**.

> **Note:** customers, accounts, transactions and loans are saved as CSV files in data/ and loaded at startup. Delete the folder’s .csv files to start fresh. Only the banking rules are saved to disk, in `data/rules.properties`.

## Roles

| Role    | Can do |
|---------|--------|
| Teller  | Register and update customers, open accounts, process transactions, record loans |
| Manager | Everything a Teller can, plus deactivate customers, freeze and close accounts, apply interest, approve loans and view the branch report |
| Admin   | Manage employees, configure rules, apply interest, view the branch report and the audit log. An Admin cannot move money. |

## Run the tests

```bash
mvn test
```

The run should end with `BUILD SUCCESS` and no failures or errors.

To run a single test class:

```bash
mvn test -Dtest=LoanServiceTest
```

In VS Code, with the Extension Pack for Java installed, you can also run the tests from the **Testing** view in the sidebar. If the tests do not appear there, open the Command Palette and run **Java: Clean Java Language Server Workspace**.

## Project structure

```
pom.xml              Maven build: Java version, JUnit, runnable jar
src/com/bank/
├── Main.java        wires everything together and starts the app
├── ui/              menus and console input
├── service/         business rules and permission checks
├── repository/      data storage
├── model/           customers, accounts, transactions, loans, employees
├── config/          banking rules
├── security/        password hashing
├── event/           event bus (services publish what happened)
├── notification/    emails sent in response to events
├── exception/       application errors
└── util/            helpers
test/com/bank/       JUnit 5 tests
data/                rules.properties, created on the first run
docs/decisions.md    answers to questions the specification leaves open
```

Maven writes its build output to `target/`, which is not committed.