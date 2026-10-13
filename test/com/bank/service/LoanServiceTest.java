package com.bank.service;

import com.bank.exception.TransactionNotAllowedException;
import com.bank.model.Loan;
import com.bank.model.LoanStatus;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;
import com.bank.testsupport.TestBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.bank.testsupport.TestBank.money;
import static org.junit.jupiter.api.Assertions.*;

class LoanServiceTest {

    private TestBank bank;
    private String ali;
    private String savings;
    private String loanId;   // 10,000 into savings, recorded by the teller, still pending

    @BeforeEach
    void setUp() {
        bank = new TestBank().asTeller();
        ali = bank.customer("Ali Khan");
        savings = bank.accounts.openSavings(ali, money("5000")).getAccountNumber();
        loanId = bank.loans.apply(ali, savings, new BigDecimal("10000")).getLoanId();
    }

    @Test
    void approvalCreditsTheAccountAndEmailsTheCustomer() {
        bank.asManager();

        bank.loans.approve(loanId);

        assertEquals(money("15000"), bank.accounts.getByNumber(savings).getBalance());
        assertEquals("Loan approved", bank.outbox.getLast().subject());
        assertEquals(TransactionType.LOAN_DISBURSEMENT, bank.transactions.history(savings).getLast().type());
    }

    @Test
    void aPendingLoanBlocksClosingItsAccount() {
        bank.asManager();

        var error = assertThrows(IllegalStateException.class, () -> bank.accounts.close(savings));
        assertTrue(error.getMessage().contains("outstanding loan"));
    }

    @Test
    void repaymentsReduceWhatIsOwedUntilTheLoanIsRepaid() {
        bank.asManager();
        bank.loans.approve(loanId);

        bank.loans.repay(loanId, new BigDecimal("4000"));
        bank.loans.repay(loanId, new BigDecimal("6000"));

        Loan loan = bank.loans.getById(loanId);
        assertEquals(LoanStatus.REPAID, loan.getStatus());
        assertEquals(money("5000"), bank.accounts.getByNumber(savings).getBalance());
        assertFalse(bank.loanRepository.hasOutstandingLoan(savings));
    }

    @Test
    void ifTheMoneyCannotArriveTheLoanStaysPending() {
        bank.asManager();
        bank.accounts.freeze(savings);

        assertThrows(TransactionNotAllowedException.class, () -> bank.loans.approve(loanId));

        assertEquals(LoanStatus.PENDING, bank.loans.getById(loanId).getStatus());
        assertEquals(TransactionStatus.FAILED, bank.transactions.history(savings).getLast().status());
    }

    @Test
    void aFixedDepositCannotReceiveALoan() {
        String fixed = bank.accounts.openFixedDeposit(ali, money("8000"), 12).getAccountNumber();

        assertThrows(TransactionNotAllowedException.class,
                () -> bank.loans.apply(ali, fixed, new BigDecimal("1000")));
    }

    @Test
    void theAccountMustBelongToTheCustomer() {
        String sara = bank.customer("Sara Ahmed");

        assertThrows(IllegalArgumentException.class,
                () -> bank.loans.apply(sara, savings, new BigDecimal("1000")));
    }

    @Test
    void inactiveCustomersCannotApply() {
        bank.asManager();
        bank.customers.deactivate(ali);
        bank.asTeller();

        assertThrows(IllegalStateException.class,
                () -> bank.loans.apply(ali, savings, new BigDecimal("1000")));
    }

    @Test
    void theBranchReportCountsLoansPerStatus() {
        String second = bank.loans.apply(ali, savings, new BigDecimal("2000")).getLoanId();
        bank.asManager();
        bank.loans.approve(loanId);
        bank.loans.reject(second);

        BranchReport report = bank.reports.branchReport();

        assertEquals(1L, report.loansByStatus().get(LoanStatus.APPROVED));
        assertEquals(1L, report.loansByStatus().get(LoanStatus.REJECTED));
        assertEquals(money("10000"), report.loansOutstanding());
    }
}