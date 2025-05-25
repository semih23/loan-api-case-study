package com.bank.loanapi.service;

import com.bank.loanapi.dto.PayLoanResponse;
import com.bank.loanapi.model.Customer;
import com.bank.loanapi.model.Loan;
import com.bank.loanapi.model.LoanInstallment;
import com.bank.loanapi.model.User;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.repository.LoanInstallmentRepository;
import com.bank.loanapi.repository.LoanRepository;
import com.bank.loanapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*; // Using wildcard for brevity as many Collection types are used

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong; // For never() with anyLong()
import static org.mockito.ArgumentMatchers.anyString; // For never() with anyString()
import static org.mockito.Mockito.*;


/**
 * Unit tests for the {@link LoanService} class.
 * This class uses Mockito to mock repository dependencies and test service logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoanService loanService;

    // --- Authentication mocks ---
    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    private User mockUser; // Represents a customer user for testing specific scenarios
    private Customer mockCustomer; // Represents a customer linked to mockUser
    private Loan mockLoan; // Represents a loan linked to mockCustomer

    /**
     * Sets up common mock objects and default behaviors before each test method.
     */
    @BeforeEach
    void setUp() {
        // Default mock behavior for an ADMIN user for authentication object.
        // Individual tests can override this for CUSTOMER role testing.
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(userDetails.getUsername()).thenReturn("admin"); // Default username for admin
        Collection<? extends GrantedAuthority> adminAuthorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        lenient().doReturn(adminAuthorities).when(authentication).getAuthorities();

        // General purpose mock objects for use in various tests
        mockUser = new User("testuser", "encodedPassword", "ROLE_CUSTOMER");
        mockUser.setId(1L); // Assume this user gets ID 1L

        mockCustomer = new Customer();
        mockCustomer.setId(1L); // Assume this customer gets ID 1L
        mockCustomer.setUser(mockUser); // Link customer to user
        mockCustomer.setCreditLimit(new BigDecimal("100000.00"));
        mockCustomer.setUsedCreditLimit(new BigDecimal("10000.00"));

        mockLoan = new Loan();
        mockLoan.setId(1L); // Assume this loan gets ID 1L
        mockLoan.setCustomer(mockCustomer); // Link loan to customer
        mockLoan.setPaid(false);
        mockLoan.setLoanAmount(new BigDecimal("1000.00")); // A default loan amount for setup
        mockLoan.setNumberOfInstallment(1); // Default to 1 installment for setup simplicity
    }

    /**
     * Test case for the createLoan method.
     * Verifies that an IllegalArgumentException is thrown when the customer's credit limit is insufficient.
     */
    @Test
    void createLoan_ShouldThrowException_WhenCreditLimitIsInsufficient() {
        // Arrange
        Customer customerWithLowLimit = new Customer();
        customerWithLowLimit.setId(1L);
        customerWithLowLimit.setCreditLimit(new BigDecimal("5000.00"));
        customerWithLowLimit.setUsedCreditLimit(new BigDecimal("0.00"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customerWithLowLimit));

        BigDecimal requestedLoanAmount = new BigDecimal("10000");
        BigDecimal interestRate = new BigDecimal("0.20");
        // Expected total with interest: 10000 * 1.20 = 12000.00

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            loanService.createLoan(1L, requestedLoanAmount, interestRate, 12);
        });

        String expectedMessage = "Customer does not have sufficient credit limit. Available: " +
                customerWithLowLimit.getCreditLimit().subtract(customerWithLowLimit.getUsedCreditLimit()).setScale(2, RoundingMode.HALF_UP).toPlainString() +
                ", Required: " +
                requestedLoanAmount.multiply(BigDecimal.ONE.add(interestRate)).setScale(2, RoundingMode.HALF_UP).toPlainString();
        assertEquals(expectedMessage, exception.getMessage());

        verify(loanRepository, never()).save(any(Loan.class));
    }

    /**
     * Test case for the createLoan method.
     * Verifies that a loan is created successfully when the customer has sufficient credit limit
     * and all input parameters are valid.
     */
    @Test
    void createLoan_ShouldCreateLoanSuccessfully_WhenCreditLimitIsSufficient() {
        // Arrange
        BigDecimal loanAmount = new BigDecimal("20000.00");
        BigDecimal interestRate = new BigDecimal("0.10");
        int numberOfInstallments = 12;
        BigDecimal totalLoanWithInterest = loanAmount.multiply(BigDecimal.ONE.add(interestRate)).setScale(2, RoundingMode.HALF_UP);


        when(customerRepository.findById(mockCustomer.getId())).thenReturn(Optional.of(mockCustomer));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Loan createdLoan = loanService.createLoan(mockCustomer.getId(), loanAmount, interestRate, numberOfInstallments);

        // Assert
        assertNotNull(createdLoan, "The created loan should not be null.");
        assertEquals(numberOfInstallments, createdLoan.getInstallments().size(), "The loan should have the correct number of installments.");
        assertEquals(mockCustomer, createdLoan.getCustomer(), "The loan should be assigned to the correct customer.");

        BigDecimal expectedUsedLimit = new BigDecimal("10000.00").add(totalLoanWithInterest).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expectedUsedLimit.compareTo(mockCustomer.getUsedCreditLimit().setScale(2, RoundingMode.HALF_UP)), "Customer's used credit limit should be updated correctly.");

        verify(customerRepository, times(1)).save(mockCustomer);
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    /**
     * Test case for the payLoan method.
     * Verifies successful payment of a single installment when the payment amount is exact and made on the due date.
     * Assumes this single payment makes the loan fully paid.
     */
    @Test
    void payLoan_ShouldPaySingleInstallment_WhenPaymentIsExactAndOnTime() {
        // Arrange
        LoanInstallment installment = new LoanInstallment();
        installment.setId(1L);
        installment.setLoan(mockLoan);
        installment.setAmount(new BigDecimal("1000.00"));
        installment.setPaid(false);
        installment.setDueDate(LocalDate.now());

        mockLoan.getInstallments().clear();
        mockLoan.getInstallments().add(installment);
        mockLoan.setNumberOfInstallment(1);
        mockLoan.setPaid(false);

        BigDecimal paymentAmount = new BigDecimal("1000.00");
        BigDecimal initialUsedCredit = mockCustomer.getUsedCreditLimit();


        when(loanRepository.findById(mockLoan.getId())).thenReturn(Optional.of(mockLoan));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(mockLoan.getId()))
                .thenReturn(Collections.singletonList(installment))
                .thenReturn(Collections.emptyList());


        // Act
        PayLoanResponse response = loanService.payLoan(mockLoan.getId(), paymentAmount, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaid(), "One installment should be paid.");
        assertEquals(0, paymentAmount.compareTo(response.getTotalAmountSpent()), "Total amount spent should match payment amount.");
        assertTrue(response.isLoanPaidCompletely(), "Loan should be marked as completely paid.");
        assertEquals("Loan has been fully paid.", response.getMessage(), "Feedback message should indicate full payment.");

        ArgumentCaptor<List<LoanInstallment>> installmentListCaptor = ArgumentCaptor.forClass(List.class);
        verify(loanInstallmentRepository).saveAll(installmentListCaptor.capture());
        List<LoanInstallment> savedInstallments = installmentListCaptor.getValue();
        assertEquals(1, savedInstallments.size());
        LoanInstallment savedInstallment = savedInstallments.get(0);

        assertTrue(savedInstallment.isPaid(), "Installment should be marked as paid.");
        assertEquals(0, installment.getAmount().compareTo(savedInstallment.getPaidAmount()), "Paid amount should be equal to installment amount.");
        assertEquals(LocalDate.now(), savedInstallment.getPaymentDate(), "Payment date should be today.");

        verify(customerRepository, times(1)).save(mockCustomer);
        BigDecimal expectedUsedCreditAfterPayment = initialUsedCredit.subtract(paymentAmount);
        assertEquals(0, expectedUsedCreditAfterPayment.compareTo(mockCustomer.getUsedCreditLimit()), "Customer's used credit limit should be updated correctly.");

        verify(loanRepository, times(1)).save(mockLoan);
        assertTrue(mockLoan.isPaid(), "Loan object itself should be marked as paid.");
    }

    /**
     * Test case for payLoan method.
     * Verifies successful payment of multiple installments, one of which receives an early payment discount.
     * Assumes these payments make the loan fully paid.
     */
    @Test
    void payLoan_ShouldPayMultipleInstallments_WhenPaymentIsSufficient() {
        // Arrange
        LoanInstallment installment1 = new LoanInstallment();
        installment1.setId(1L);
        installment1.setLoan(mockLoan);
        installment1.setAmount(new BigDecimal("1000.00"));
        installment1.setPaid(false);
        installment1.setDueDate(LocalDate.now()); // On-time payment, no discount/penalty

        LoanInstallment installment2 = new LoanInstallment();
        installment2.setId(2L);
        installment2.setLoan(mockLoan);
        installment2.setAmount(new BigDecimal("1200.00"));
        installment2.setPaid(false);
        installment2.setDueDate(LocalDate.now().plusDays(1)); // 1 day early payment

        mockLoan.getInstallments().clear();
        mockLoan.getInstallments().add(installment1);
        mockLoan.getInstallments().add(installment2);
        mockLoan.setNumberOfInstallment(2);
        mockLoan.setPaid(false);

        BigDecimal paymentAmountSent = new BigDecimal("2200.00");
        BigDecimal initialUsedCredit = mockCustomer.getUsedCreditLimit();

        BigDecimal expectedInst1Payment = installment1.getAmount();
        long daysBeforeInst2 = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), installment2.getDueDate());
        BigDecimal discountInst2 = installment2.getAmount().multiply(new BigDecimal("0.001")).multiply(new BigDecimal(daysBeforeInst2));
        BigDecimal expectedInst2PaymentWithDiscount = installment2.getAmount().subtract(discountInst2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTotalSpentWithDiscount = expectedInst1Payment.add(expectedInst2PaymentWithDiscount).setScale(2, RoundingMode.HALF_UP);

        when(loanRepository.findById(mockLoan.getId())).thenReturn(Optional.of(mockLoan));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(mockLoan.getId()))
                .thenReturn(List.of(installment1, installment2))
                .thenReturn(Collections.emptyList());

        // Act
        PayLoanResponse response = loanService.payLoan(mockLoan.getId(), paymentAmountSent, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getInstallmentsPaid(), "Two installments should be paid.");
        assertTrue(response.isLoanPaidCompletely(), "Loan should be marked as completely paid.");
        // Since the first paid installment (installment1) is on time, no discount message for it.
        // The "Loan has been fully paid." message takes precedence if no specific message for the *first* paid installment.
        assertEquals("Loan has been fully paid.", response.getMessage(), "Feedback message should indicate full payment.");
        assertEquals(0, expectedTotalSpentWithDiscount.compareTo(response.getTotalAmountSpent()), "Total amount spent should be " + expectedTotalSpentWithDiscount + " including discount.");

        ArgumentCaptor<List<LoanInstallment>> captor = ArgumentCaptor.forClass(List.class);
        verify(loanInstallmentRepository).saveAll(captor.capture());
        List<LoanInstallment> savedInstallments = captor.getValue();
        assertEquals(2, savedInstallments.size());

        LoanInstallment firstPaid = savedInstallments.stream().filter(inst -> inst.getId().equals(1L)).findFirst().orElseThrow();
        assertTrue(firstPaid.isPaid());
        assertEquals(0, installment1.getAmount().compareTo(firstPaid.getPaidAmount()));

        LoanInstallment secondPaid = savedInstallments.stream().filter(inst -> inst.getId().equals(2L)).findFirst().orElseThrow();
        assertTrue(secondPaid.isPaid());
        assertEquals(0, expectedInst2PaymentWithDiscount.compareTo(secondPaid.getPaidAmount()));

        verify(customerRepository, times(1)).save(mockCustomer);
        BigDecimal expectedUsedCreditAfterPayment = initialUsedCredit.subtract(expectedTotalSpentWithDiscount);
        assertEquals(0, expectedUsedCreditAfterPayment.compareTo(mockCustomer.getUsedCreditLimit().setScale(2, RoundingMode.HALF_UP)), "Customer's used credit limit should be updated correctly.");

        verify(loanRepository, times(1)).save(mockLoan);
        assertTrue(mockLoan.isPaid());
    }

    /**
     * Test case for payLoan method.
     * Verifies that no installments are paid and an appropriate message is returned
     * when the payment amount is insufficient for the first due installment.
     */
    @Test
    void payLoan_ShouldFail_WhenPaymentIsInsufficientForFirstInstallment() {
        // Arrange
        LoanInstallment installment = new LoanInstallment();
        installment.setId(1L);
        installment.setLoan(mockLoan);
        installment.setAmount(new BigDecimal("1000.00"));
        installment.setPaid(false);
        installment.setDueDate(LocalDate.now());

        mockLoan.getInstallments().clear();
        mockLoan.getInstallments().add(installment);
        mockLoan.setNumberOfInstallment(1);
        mockLoan.setPaid(false);

        BigDecimal insufficientPaymentAmount = new BigDecimal("900.00");

        when(loanRepository.findById(mockLoan.getId())).thenReturn(Optional.of(mockLoan));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(mockLoan.getId()))
                .thenReturn(Collections.singletonList(installment));

        // Act
        PayLoanResponse response = loanService.payLoan(mockLoan.getId(), insufficientPaymentAmount, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getInstallmentsPaid(), "No installments should be paid.");
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalAmountSpent()), "Total amount spent should be zero.");
        assertFalse(response.isLoanPaidCompletely(), "Loan should not be marked as completely paid.");

        String expectedMessage = "Payment amount is insufficient to cover the principal of the first due installment (Amount: " +
                installment.getAmount().setScale(2, RoundingMode.HALF_UP) + ").";
        assertEquals(expectedMessage, response.getMessage(), "Feedback message should indicate insufficient payment for principal.");

        verify(loanInstallmentRepository, never()).saveAll(anyList());
        verify(customerRepository, never()).save(any(Customer.class));
        verify(loanRepository, never()).save(any(Loan.class));

        assertFalse(installment.isPaid());
        assertNull(installment.getPaidAmount());
        assertNull(installment.getPaymentDate());
    }

    /**
     * Test case for payLoan method.
     * Verifies that only installments within the 3-month payment window are paid.
     */
    @Test
    void payLoan_ShouldOnlyPayInstallments_WithinPaymentWindow() {
        // Arrange
        LoanInstallment payableInstallment = new LoanInstallment();
        payableInstallment.setId(1L);
        payableInstallment.setLoan(mockLoan);
        payableInstallment.setAmount(new BigDecimal("1000.00"));
        payableInstallment.setPaid(false);
        payableInstallment.setDueDate(LocalDate.now().plusMonths(2)); // Payable (2 months from now)

        LoanInstallment futureInstallment = new LoanInstallment();
        futureInstallment.setId(2L);
        futureInstallment.setLoan(mockLoan);
        futureInstallment.setAmount(new BigDecimal("1200.00"));
        futureInstallment.setPaid(false);
        futureInstallment.setDueDate(LocalDate.now().plusMonths(4)); // Not payable (4 months from now)

        mockLoan.getInstallments().clear();
        mockLoan.getInstallments().add(payableInstallment);
        mockLoan.getInstallments().add(futureInstallment);
        mockLoan.setNumberOfInstallment(2);
        mockLoan.setPaid(false);

        BigDecimal paymentAmountSent = new BigDecimal("2200.00"); // Enough to cover both if allowed
        BigDecimal initialUsedCredit = mockCustomer.getUsedCreditLimit();

        when(loanRepository.findById(mockLoan.getId())).thenReturn(Optional.of(mockLoan));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(mockLoan.getId()))
                .thenReturn(List.of(payableInstallment, futureInstallment));
        // After payment, only the futureInstallment might remain if the first was paid
        // Or if the first wasn't paid due to window rule, then both remain.
        // Service logic breaks loop if dueDate.isAfter(paymentWindowEnd)

        // Act
        PayLoanResponse response = loanService.payLoan(mockLoan.getId(), paymentAmountSent, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaid(), "Only one installment (within the payment window) should be paid.");
        assertFalse(response.isLoanPaidCompletely(), "Loan should not be completely paid as one installment remains.");

        long daysBeforePayableInst = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), payableInstallment.getDueDate());
        BigDecimal discountPayableInst = payableInstallment.getAmount().multiply(new BigDecimal("0.001")).multiply(new BigDecimal(daysBeforePayableInst));
        BigDecimal expectedPaymentForPayableInst = payableInstallment.getAmount().subtract(discountPayableInst).setScale(2, RoundingMode.HALF_UP);

        assertEquals(0, expectedPaymentForPayableInst.compareTo(response.getTotalAmountSpent()), "Total amount spent should be for the first (payable) installment only, including discount.");
        if (LocalDate.now().isBefore(payableInstallment.getDueDate())) {
            assertEquals("An early payment discount was applied to your payment.", response.getMessage());
        } else {
            assertNull(response.getMessage(), "No specific message expected if the only paid installment is on time/late and others are out of window.");
        }

        ArgumentCaptor<List<LoanInstallment>> captor = ArgumentCaptor.forClass(List.class);
        verify(loanInstallmentRepository).saveAll(captor.capture());
        List<LoanInstallment> savedInstallments = captor.getValue();
        assertEquals(1, savedInstallments.size());
        assertTrue(savedInstallments.get(0).isPaid());
        assertEquals(payableInstallment.getId(), savedInstallments.get(0).getId());

        verify(customerRepository, times(1)).save(mockCustomer);
        BigDecimal expectedUsedCreditAfterPayment = initialUsedCredit.subtract(expectedPaymentForPayableInst);
        assertEquals(0, expectedUsedCreditAfterPayment.compareTo(mockCustomer.getUsedCreditLimit().setScale(2, RoundingMode.HALF_UP)), "Customer's used credit limit should be updated.");

        verify(loanRepository, never()).save(mockLoan); // Loan is not fully paid
        assertFalse(mockLoan.isPaid());
    }

    /**
     * Test case for payLoan method.
     * Verifies that an early payment discount is applied for a single installment paid before its due date.
     * Assumes this payment makes the loan fully paid.
     */
    @Test
    void payLoan_ShouldApplyDiscount_ForEarlySingleInstallmentPayment() {
        // Arrange
        LocalDate dueDate = LocalDate.now().plusDays(10);
        LoanInstallment earlyInstallment = new LoanInstallment();
        earlyInstallment.setId(1L);
        earlyInstallment.setLoan(mockLoan);
        earlyInstallment.setAmount(new BigDecimal("1000.00"));
        earlyInstallment.setPaid(false);
        earlyInstallment.setDueDate(dueDate);

        mockLoan.getInstallments().clear();
        mockLoan.getInstallments().add(earlyInstallment);
        mockLoan.setNumberOfInstallment(1);
        mockLoan.setPaid(false);

        BigDecimal paymentAmountSent = new BigDecimal("1000.00");
        BigDecimal initialUsedCredit = mockCustomer.getUsedCreditLimit();

        long daysBefore = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        BigDecimal discount = earlyInstallment.getAmount().multiply(new BigDecimal("0.001")).multiply(new BigDecimal(daysBefore));
        BigDecimal expectedPaidAmountWithDiscount = earlyInstallment.getAmount().subtract(discount).setScale(2, RoundingMode.HALF_UP);

        when(loanRepository.findById(mockLoan.getId())).thenReturn(Optional.of(mockLoan));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(mockLoan.getId()))
                .thenReturn(Collections.singletonList(earlyInstallment))
                .thenReturn(Collections.emptyList());

        // Act
        PayLoanResponse response = loanService.payLoan(mockLoan.getId(), paymentAmountSent, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaid());
        assertTrue(response.isLoanPaidCompletely());
        assertEquals("An early payment discount was applied to your payment.", response.getMessage());
        assertEquals(0, expectedPaidAmountWithDiscount.compareTo(response.getTotalAmountSpent()));

        ArgumentCaptor<List<LoanInstallment>> captor = ArgumentCaptor.forClass(List.class);
        verify(loanInstallmentRepository).saveAll(captor.capture());
        LoanInstallment savedInstallment = captor.getValue().get(0);

        assertTrue(savedInstallment.isPaid());
        assertEquals(0, expectedPaidAmountWithDiscount.compareTo(savedInstallment.getPaidAmount()));
        assertEquals(LocalDate.now(), savedInstallment.getPaymentDate());

        verify(customerRepository, times(1)).save(mockCustomer);
        BigDecimal expectedUsedCreditAfterPayment = initialUsedCredit.subtract(expectedPaidAmountWithDiscount);
        assertEquals(0, expectedUsedCreditAfterPayment.compareTo(mockCustomer.getUsedCreditLimit().setScale(2, RoundingMode.HALF_UP)));

        verify(loanRepository, times(1)).save(mockLoan);
        assertTrue(mockLoan.isPaid());
    }

    /**
     * Test case for payLoan method.
     * Verifies that a late payment penalty is applied for a single installment paid after its due date.
     * Assumes this payment makes the loan fully paid.
     */
    @Test
    void payLoan_ShouldApplyPenalty_ForLateSingleInstallmentPayment() {
        // Arrange
        LocalDate dueDate = LocalDate.now().minusDays(10);
        LoanInstallment lateInstallment = new LoanInstallment();
        lateInstallment.setId(1L);
        lateInstallment.setLoan(mockLoan);
        lateInstallment.setAmount(new BigDecimal("1000.00"));
        lateInstallment.setPaid(false);
        lateInstallment.setDueDate(dueDate);

        mockLoan.getInstallments().clear();
        mockLoan.getInstallments().add(lateInstallment);
        mockLoan.setNumberOfInstallment(1);
        mockLoan.setPaid(false);

        long daysAfter = java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        BigDecimal penalty = lateInstallment.getAmount().multiply(new BigDecimal("0.001")).multiply(new BigDecimal(daysAfter));
        BigDecimal expectedPaidAmountWithPenalty = lateInstallment.getAmount().add(penalty).setScale(2, RoundingMode.HALF_UP);

        BigDecimal paymentAmountSent = expectedPaidAmountWithPenalty; // Pay exact penalized amount
        BigDecimal initialUsedCredit = mockCustomer.getUsedCreditLimit();

        when(loanRepository.findById(mockLoan.getId())).thenReturn(Optional.of(mockLoan));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(mockLoan.getId()))
                .thenReturn(Collections.singletonList(lateInstallment))
                .thenReturn(Collections.emptyList());

        // Act
        PayLoanResponse response = loanService.payLoan(mockLoan.getId(), paymentAmountSent, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaid());
        assertTrue(response.isLoanPaidCompletely());
        // Since the first (and only) paid installment was late, no "early discount" message.
        // "Loan fully paid" message takes precedence.
        assertEquals("Loan has been fully paid.", response.getMessage());
        assertEquals(0, expectedPaidAmountWithPenalty.compareTo(response.getTotalAmountSpent()));

        ArgumentCaptor<List<LoanInstallment>> captor = ArgumentCaptor.forClass(List.class);
        verify(loanInstallmentRepository).saveAll(captor.capture());
        LoanInstallment savedInstallment = captor.getValue().get(0);

        assertTrue(savedInstallment.isPaid());
        assertEquals(0, expectedPaidAmountWithPenalty.compareTo(savedInstallment.getPaidAmount()));
        assertEquals(LocalDate.now(), savedInstallment.getPaymentDate());

        verify(customerRepository, times(1)).save(mockCustomer);
        BigDecimal expectedUsedCreditAfterPayment = initialUsedCredit.subtract(expectedPaidAmountWithPenalty);
        assertEquals(0, expectedUsedCreditAfterPayment.compareTo(mockCustomer.getUsedCreditLimit().setScale(2, RoundingMode.HALF_UP)));

        verify(loanRepository, times(1)).save(mockLoan);
        assertTrue(mockLoan.isPaid());
    }

    /**
     * Test case for listLoansByCustomer method.
     * Verifies that an ADMIN user can list loans for any customer.
     */
    @Test
    void listLoansByCustomer_AsAdmin_ShouldReturnLoansForAnyCustomer() {
        // Arrange
        Long targetCustomerId = 2L;
        Customer targetCustomer = new Customer();
        targetCustomer.setId(targetCustomerId);
        // setUp() configures authentication as ADMIN by default

        Loan loan1 = new Loan(); loan1.setId(10L); loan1.setCustomer(targetCustomer);
        Loan loan2 = new Loan(); loan2.setId(11L); loan2.setCustomer(targetCustomer);
        List<Loan> expectedLoans = List.of(loan1, loan2);

        when(customerRepository.findById(targetCustomerId)).thenReturn(Optional.of(targetCustomer));
        when(loanRepository.findByCustomer_Id(targetCustomerId)).thenReturn(expectedLoans);

        // Act
        List<Loan> actualLoans = loanService.listLoansByCustomer(targetCustomerId, authentication);

        // Assert
        assertNotNull(actualLoans);
        assertEquals(2, actualLoans.size());
        assertEquals(expectedLoans, actualLoans);
        verify(userRepository, never()).findByUsername(anyString());
        verify(customerRepository, never()).findByUser_Id(anyLong());
        verify(customerRepository, times(1)).findById(targetCustomerId);
        verify(loanRepository, times(1)).findByCustomer_Id(targetCustomerId);
    }

    /**
     * Test case for listLoansByCustomer method.
     * Verifies that a CUSTOMER user can only list their own loans.
     */
    @Test
    void listLoansByCustomer_AsCustomer_ShouldReturnOwnLoans() {
        // Arrange
        Long ownCustomerId = mockCustomer.getId(); // Should be 1L from setUp

        // Configure authentication for CUSTOMER role
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(mockUser.getUsername()); // "testuser"
        Collection<? extends GrantedAuthority> customerAuthorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        doReturn(customerAuthorities).when(authentication).getAuthorities();

        when(userRepository.findByUsername(mockUser.getUsername())).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByUser_Id(mockUser.getId())).thenReturn(Optional.of(mockCustomer));
        when(customerRepository.findById(ownCustomerId)).thenReturn(Optional.of(mockCustomer));

        Loan loan1 = new Loan(); loan1.setId(20L); loan1.setCustomer(mockCustomer);
        List<Loan> expectedLoans = List.of(loan1);
        when(loanRepository.findByCustomer_Id(ownCustomerId)).thenReturn(expectedLoans);

        // Act
        List<Loan> actualLoans = loanService.listLoansByCustomer(ownCustomerId, authentication);

        // Assert
        assertNotNull(actualLoans);
        assertEquals(1, actualLoans.size());
        assertEquals(expectedLoans, actualLoans);
        verify(userRepository, times(1)).findByUsername(mockUser.getUsername());
        verify(customerRepository, times(1)).findByUser_Id(mockUser.getId());
        verify(customerRepository, times(1)).findById(ownCustomerId);
        verify(loanRepository, times(1)).findByCustomer_Id(ownCustomerId);
    }

    /**
     * Test case for listInstallmentsByLoan method.
     * Verifies that an ADMIN user can list installments for any loan.
     */
    @Test
    void listInstallmentsByLoan_AsAdmin_ShouldReturnInstallmentsForAnyLoan() {
        // Arrange
        Long targetLoanId = 2L;
        Loan specificLoan = new Loan();
        specificLoan.setId(targetLoanId);
        specificLoan.setCustomer(mockCustomer); // Associated with some customer

        LoanInstallment inst1 = new LoanInstallment(); inst1.setId(100L); inst1.setLoan(specificLoan);
        LoanInstallment inst2 = new LoanInstallment(); inst2.setId(101L); inst2.setLoan(specificLoan);
        specificLoan.setInstallments(new ArrayList<>(List.of(inst1, inst2)));
        // setUp() configures authentication as ADMIN by default

        when(loanRepository.findById(targetLoanId)).thenReturn(Optional.of(specificLoan));

        // Act
        List<LoanInstallment> actualInstallments = loanService.listInstallmentsByLoan(targetLoanId, authentication);

        // Assert
        assertNotNull(actualInstallments);
        assertEquals(2, actualInstallments.size());
        assertTrue(actualInstallments.stream().anyMatch(i -> i.getId().equals(100L)));
        assertTrue(actualInstallments.stream().anyMatch(i -> i.getId().equals(101L)));
        verify(userRepository, never()).findByUsername(anyString());
        verify(loanRepository, times(1)).findById(targetLoanId);
    }

    /**
     * Test case for listInstallmentsByLoan method.
     * Verifies that a CUSTOMER user can only list installments for their own loans.
     */
    @Test
    void listInstallmentsByLoan_AsCustomer_ShouldReturnOwnLoanInstallments() {
        // Arrange
        Long targetLoanId = mockLoan.getId(); // Use loan associated with mockUser/mockCustomer from setUp

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(mockUser.getUsername()); // "testuser"
        Collection<? extends GrantedAuthority> customerAuthorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        doReturn(customerAuthorities).when(authentication).getAuthorities();

        LoanInstallment inst1 = new LoanInstallment(); inst1.setId(30L); inst1.setLoan(mockLoan);
        LoanInstallment inst2 = new LoanInstallment(); inst2.setId(31L); inst2.setLoan(mockLoan);
        mockLoan.getInstallments().clear();
        mockLoan.setInstallments(new ArrayList<>(List.of(inst1, inst2)));

        when(loanRepository.findById(targetLoanId)).thenReturn(Optional.of(mockLoan));
        when(userRepository.findByUsername(mockUser.getUsername())).thenReturn(Optional.of(mockUser));

        // Act
        List<LoanInstallment> actualInstallments = loanService.listInstallmentsByLoan(targetLoanId, authentication);

        // Assert
        assertNotNull(actualInstallments);
        assertEquals(2, actualInstallments.size());
        assertTrue(actualInstallments.stream().anyMatch(i -> i.getId().equals(30L)));
        assertTrue(actualInstallments.stream().anyMatch(i -> i.getId().equals(31L)));
        verify(loanRepository, times(1)).findById(targetLoanId);
        verify(userRepository, times(1)).findByUsername(mockUser.getUsername());
    }

    /**
     * Test case for listInstallmentsByLoan method.
     * Verifies that a CUSTOMER user receives AccessDeniedException when trying to list installments for another customer's loan.
     */
    @Test
    void listInstallmentsByLoan_AsCustomer_ShouldThrowAccessDenied_ForOtherCustomersLoan() {
        // Arrange
        Long otherCustomersLoanId = 2L;

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(mockUser.getUsername()); // "testuser" (owns loan 1)
        Collection<? extends GrantedAuthority> customerAuthorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        doReturn(customerAuthorities).when(authentication).getAuthorities();

        User otherUser = new User("otheruser", "password", "ROLE_CUSTOMER"); otherUser.setId(2L);
        Customer otherCustomer = new Customer(); otherCustomer.setId(2L); otherCustomer.setUser(otherUser);
        Loan otherCustomersLoan = new Loan(); otherCustomersLoan.setId(otherCustomersLoanId); otherCustomersLoan.setCustomer(otherCustomer);

        when(loanRepository.findById(otherCustomersLoanId)).thenReturn(Optional.of(otherCustomersLoan));
        when(userRepository.findByUsername(mockUser.getUsername())).thenReturn(Optional.of(mockUser));

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            loanService.listInstallmentsByLoan(otherCustomersLoanId, authentication);
        });
        assertEquals("You are not authorized to view installments for this loan.", exception.getMessage());
        verify(loanInstallmentRepository, never()).findByLoan_Id(anyLong());
    }

    /**
     * Test case for payLoan method.
     * Verifies that an ADMIN user can make payments for any loan.
     */
    @Test
    void payLoan_AsAdmin_ShouldAllowPaymentForAnyLoan() {
        // Arrange
        Long targetLoanId = mockLoan.getId();
        LoanInstallment installment = new LoanInstallment();
        installment.setId(50L); installment.setLoan(mockLoan); installment.setAmount(new BigDecimal("500.00"));
        installment.setPaid(false); installment.setDueDate(LocalDate.now());
        mockLoan.getInstallments().clear(); mockLoan.getInstallments().add(installment);
        mockLoan.setNumberOfInstallment(1); mockLoan.setPaid(false);

        BigDecimal paymentAmount = new BigDecimal("500.00");
        BigDecimal initialUsedCredit = mockCustomer.getUsedCreditLimit();

        when(loanRepository.findById(targetLoanId)).thenReturn(Optional.of(mockLoan));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(targetLoanId))
                .thenReturn(Collections.singletonList(installment))
                .thenReturn(Collections.emptyList());
        // Act
        PayLoanResponse response = loanService.payLoan(targetLoanId, paymentAmount, authentication); // Admin auth from setUp

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaid());
        assertTrue(response.isLoanPaidCompletely());
        assertEquals(0, paymentAmount.compareTo(response.getTotalAmountSpent()));
        assertEquals("Loan has been fully paid.", response.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(loanInstallmentRepository).saveAll(anyList());
        verify(customerRepository).save(mockLoan.getCustomer());
        verify(loanRepository).save(mockLoan);
        BigDecimal expectedUsedCredit = initialUsedCredit.subtract(paymentAmount);
        assertEquals(0, expectedUsedCredit.compareTo(mockCustomer.getUsedCreditLimit().setScale(2, RoundingMode.HALF_UP)));
    }

    /**
     * Test case for payLoan method.
     * Verifies that a CUSTOMER user can make payments for their own loan.
     */
    @Test
    void payLoan_AsCustomer_ShouldAllowPaymentForOwnLoan() {
        // Arrange
        Long ownLoanId = mockLoan.getId();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(mockUser.getUsername()); // "testuser"
        Collection<? extends GrantedAuthority> customerAuthorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        doReturn(customerAuthorities).when(authentication).getAuthorities();

        LoanInstallment installment = new LoanInstallment();
        installment.setId(60L); installment.setLoan(mockLoan); installment.setAmount(new BigDecimal("700.00"));
        installment.setPaid(false); installment.setDueDate(LocalDate.now());
        mockLoan.getInstallments().clear(); mockLoan.getInstallments().add(installment);
        mockLoan.setNumberOfInstallment(1); mockLoan.setPaid(false);

        BigDecimal paymentAmount = new BigDecimal("700.00");
        BigDecimal initialUsedCredit = mockCustomer.getUsedCreditLimit();

        when(loanRepository.findById(ownLoanId)).thenReturn(Optional.of(mockLoan));
        when(userRepository.findByUsername(mockUser.getUsername())).thenReturn(Optional.of(mockUser));
        when(loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(ownLoanId))
                .thenReturn(Collections.singletonList(installment))
                .thenReturn(Collections.emptyList());

        // Act
        PayLoanResponse response = loanService.payLoan(ownLoanId, paymentAmount, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaid());
        assertTrue(response.isLoanPaidCompletely());
        assertEquals(0, paymentAmount.compareTo(response.getTotalAmountSpent()));
        assertEquals("Loan has been fully paid.", response.getMessage());
        verify(userRepository, times(1)).findByUsername(mockUser.getUsername());
        verify(loanInstallmentRepository).saveAll(anyList());
        verify(customerRepository).save(mockLoan.getCustomer());
        verify(loanRepository).save(mockLoan);
        BigDecimal expectedUsedCredit = initialUsedCredit.subtract(paymentAmount);
        assertEquals(0, expectedUsedCredit.compareTo(mockCustomer.getUsedCreditLimit().setScale(2, RoundingMode.HALF_UP)));
    }

    /**
     * Test case for payLoan method.
     * Verifies that a CUSTOMER user receives AccessDeniedException when trying to pay for another customer's loan.
     */
    @Test
    void payLoan_AsCustomer_ShouldThrowAccessDenied_ForOtherCustomersLoan() {
        // Arrange
        Long otherCustomersLoanId = 2L;

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(mockUser.getUsername()); // "testuser"
        Collection<? extends GrantedAuthority> customerAuthorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        doReturn(customerAuthorities).when(authentication).getAuthorities();

        User otherUser = new User("otheruser", "password", "ROLE_CUSTOMER"); otherUser.setId(2L);
        Customer otherCustomer = new Customer(); otherCustomer.setId(2L); otherCustomer.setUser(otherUser);
        Loan otherCustomersLoan = new Loan(); otherCustomersLoan.setId(otherCustomersLoanId); otherCustomersLoan.setCustomer(otherCustomer);
        otherCustomersLoan.setPaid(false); // Ensure loan is not already paid
        // Add a dummy installment to avoid issues if the service tries to fetch them before access check
        LoanInstallment dummyInstallment = new LoanInstallment(); dummyInstallment.setAmount(BigDecimal.TEN);
        otherCustomersLoan.setInstallments(Collections.singletonList(dummyInstallment));


        when(loanRepository.findById(otherCustomersLoanId)).thenReturn(Optional.of(otherCustomersLoan));
        when(userRepository.findByUsername(mockUser.getUsername())).thenReturn(Optional.of(mockUser));

        BigDecimal paymentAmount = new BigDecimal("100.00");

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            loanService.payLoan(otherCustomersLoanId, paymentAmount, authentication);
        });
        assertEquals("You are not authorized to make a payment for this loan.", exception.getMessage());
        verify(loanInstallmentRepository, never()).saveAll(anyList());
        verify(customerRepository, never()).save(any(Customer.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }
}