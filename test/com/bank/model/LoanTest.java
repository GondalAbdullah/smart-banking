package com.bank.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

class LoanTest {

    private final Loan loan = Loan.apply("L0001", "C0001", "A00001",
            new BigDecimal("10000"), LocalDate.of(2026, 10, 15));

    @Test
    void newLoanIsPendingAndOwesNothing() {
        assertEquals(LoanStatus.PENDING, loan.getStatus());
        assertEquals(money("0"), loan.getOutstanding());
    }

    @Test
    void approvalMakesThePrincipalOutstanding() {
        loan.approve();

        assertEquals(LoanStatus.APPROVED, loan.getStatus());
        assertEquals(money("10000"), loan.getOutstanding());
    }

    @Test
    void cannotBeDecidedTwice() {
        loan.approve();

        assertThrows(IllegalStateException.class, loan::approve);
        assertThrows(IllegalStateException.class, loan::reject);
    }

    @Test
    void cannotRepayALoanThatIsNotApproved() {
        assertThrows(IllegalStateException.class, () -> loan.repay(new BigDecimal("100")));
    }

    @Test
    void cannotRepayMoreThanIsOutstanding() {
        loan.approve();

        assertThrows(IllegalArgumentException.class, () -> loan.repay(new BigDecimal("10000.01")));
    }

    @Test
    void repayingEverythingMarksTheLoanRepaid() {
        loan.approve();

        loan.repay(new BigDecimal("4000"));
        assertEquals(money("6000"), loan.getOutstanding());
        assertEquals(LoanStatus.APPROVED, loan.getStatus());

        loan.repay(new BigDecimal("6000"));
        assertEquals(LoanStatus.REPAID, loan.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING,  true",
            "APPROVED, true",
            "REJECTED, false",
            "REPAID,   false"
    })
    void onlyPendingAndApprovedLoansAreLiabilities(LoanStatus status, boolean liability) {
        BigDecimal outstanding = status == LoanStatus.APPROVED ? new BigDecimal("500") : BigDecimal.ZERO;
        Loan restored = Loan.restore("L0002", "C0001", "A00001", new BigDecimal("1000"),
                LocalDate.of(2026, 10, 15), outstanding, status);

        assertEquals(liability, restored.isLiability());
    }
}