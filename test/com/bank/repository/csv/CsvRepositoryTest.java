package com.bank.repository.csv;

import com.bank.model.Account;
import com.bank.model.AccountStatus;
import com.bank.model.CurrentAccount;
import com.bank.model.Customer;
import com.bank.model.FixedDepositAccount;
import com.bank.model.SavingsAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

/** Saves through one repository, then reads the same file with a brand-new one. */
class CsvRepositoryTest {

    private static final LocalDate OCT_15 = LocalDate.of(2026, 10, 15);

    @TempDir
    Path folder;

    @Test
    void aCustomerWithACommaInTheAddressComesBackUnchanged() {
        Path file = folder.resolve("customers.csv");
        new CsvCustomerRepository(file).add(Customer.restore("C0001", "Ali Khan", "ali@example.com",
                "03001234567", "House 5, Street 2, \"Model Town\"", OCT_15, com.bank.model.CustomerStatus.ACTIVE));

        Customer loaded = new CsvCustomerRepository(file).findById("C0001").orElseThrow();

        assertEquals("House 5, Street 2, \"Model Town\"", loaded.getAddress());
        assertEquals(OCT_15, loaded.getRegistrationDate());
        assertEquals("C0002", new CsvCustomerRepository(file).nextId());
    }

    @Test
    void everyAccountTypeComesBackWithItsOwnFields() {
        Path file = folder.resolve("accounts.csv");
        CsvAccountRepository first = new CsvAccountRepository(file);
        SavingsAccount savings = SavingsAccount.open("A00001", "C0001", new BigDecimal("5000"),
                new BigDecimal("1000"), new BigDecimal("0.04"), 3, OCT_15);
        savings.withdraw(new BigDecimal("100"), OCT_15);
        first.add(savings);
        first.add(CurrentAccount.open("A00002", "C0001", new BigDecimal("100"), new BigDecimal("10000"), OCT_15));
        first.add(FixedDepositAccount.open("A00003", "C0001", new BigDecimal("8000"), new BigDecimal("0.07"), 12, OCT_15));

        CsvAccountRepository second = new CsvAccountRepository(file);

        SavingsAccount s = (SavingsAccount) second.findByNumber("A00001").orElseThrow();
        CurrentAccount c = (CurrentAccount) second.findByNumber("A00002").orElseThrow();
        FixedDepositAccount f = (FixedDepositAccount) second.findByNumber("A00003").orElseThrow();
        assertAll(
                () -> assertEquals(money("4900"), s.getBalance()),
                () -> assertEquals(2, s.remainingWithdrawals(OCT_15)),   // the used withdrawal is remembered
                () -> assertEquals(new BigDecimal("1000"), s.getMinimumBalance()),
                () -> assertEquals(new BigDecimal("10000"), c.getOverdraftLimit()),
                () -> assertEquals(OCT_15.plusMonths(12), f.getMaturityDate()),
                () -> assertEquals("A00004", second.nextAccountNumber()));
    }

    @Test
    void anUpdateIsSaved() {
        Path file = folder.resolve("accounts.csv");
        CsvAccountRepository first = new CsvAccountRepository(file);
        Account account = CurrentAccount.open("A00001", "C0001", new BigDecimal("100"), BigDecimal.ZERO, OCT_15);
        first.add(account);

        account.freeze();
        first.update(account);

        assertEquals(AccountStatus.FROZEN,
                new CsvAccountRepository(file).findByNumber("A00001").orElseThrow().getStatus());
    }

    @Test
    void aMissingFileMeansNoDataYet() {
        CsvCustomerRepository repository = new CsvCustomerRepository(folder.resolve("customers.csv"));

        assertTrue(repository.findAll().isEmpty());
        assertFalse(Files.exists(folder.resolve("customers.csv")));   // nothing is written until something changes
    }

    @Test
    void aBadValueNamesTheFileLineAndColumn() throws IOException {
        Path file = folder.resolve("customers.csv");
        Files.writeString(file, """
                customerId,fullName,email,phone,address,registrationDate,status
                C0001,Ali Khan,ali@example.com,03001234567,Lahore,2026-10-15,ACTIVE
                C0002,Sara Ahmed,sara@example.com,03001234567,Lahore,15/10/2026,ACTIVE
                """);

        var error = assertThrows(IllegalStateException.class, () -> new CsvCustomerRepository(file));

        assertTrue(error.getMessage().contains("line 3"), error.getMessage());
        assertTrue(error.getMessage().contains("registrationDate '15/10/2026'"), error.getMessage());
    }

    @Test
    void theModelsOwnValidationStillRunsOnLoad() throws IOException {
        Path file = folder.resolve("customers.csv");
        Files.writeString(file, """
                customerId,fullName,email,phone,address,registrationDate,status
                C0001,Ali Khan,not-an-email,03001234567,Lahore,2026-10-15,ACTIVE
                """);

        var error = assertThrows(IllegalStateException.class, () -> new CsvCustomerRepository(file));

        assertTrue(error.getMessage().contains("line 2: Invalid email"), error.getMessage());
    }

    @Test
    void aFileWithDifferentColumnsIsRefused() throws IOException {
        Path file = folder.resolve("customers.csv");
        Files.writeString(file, "customerId,email,fullName,phone,address,registrationDate,status\n");

        var error = assertThrows(IllegalStateException.class, () -> new CsvCustomerRepository(file));

        assertTrue(error.getMessage().contains("unexpected header"));
    }
}
