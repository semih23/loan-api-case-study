package com.bank.loanapi.service;

import com.bank.loanapi.dto.PayLoanResponse;
import com.bank.loanapi.model.Customer;
import com.bank.loanapi.model.Loan;
import com.bank.loanapi.model.LoanInstallment;
import com.bank.loanapi.model.User;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.repository.LoanRepository;
import com.bank.loanapi.repository.LoanInstallmentRepository;
import com.bank.loanapi.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for handling loan-related business logic.
 * This includes creating loans, listing loans and installments, and processing loan payments.
 */
@Service
public class LoanService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final UserRepository userRepository;

    /**
     * Constructor for LoanService.
     * Injects dependencies for various repositories using constructor injection.
     * Spring will automatically provide instances of these repositories.
     *
     * @param customerRepository      Repository for customer data operations.
     * @param loanRepository          Repository for loan data operations.
     * @param loanInstallmentRepository Repository for loan installment data operations.
     * @param userRepository          Repository for user data operations.
     */
    public LoanService(CustomerRepository customerRepository,
                       LoanRepository loanRepository,
                       LoanInstallmentRepository loanInstallmentRepository,
                       UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
        this.loanInstallmentRepository = loanInstallmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new loan for a specified customer.
     * Validates input parameters, checks customer's credit limit, calculates installment details,
     * updates the customer's used credit limit, and saves the loan and its installments.
     *
     * @param customerId            The ID of the customer taking the loan.
     * @param amount                The principal amount of the loan.
     * @param interestRate          The interest rate for the loan (e.g., 0.1 for 10%).
     * @param numberOfInstallments  The number of installments for the loan.
     * @return The created Loan object with its generated installments.
     * @throws IllegalArgumentException if input validation fails or customer limit is insufficient.
     */
    @Transactional
    public Loan createLoan(Long customerId, BigDecimal amount, BigDecimal interestRate, int numberOfInstallments) {
        // 1. Input Validations
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be positive.");
        }
        if (interestRate == null || interestRate.compareTo(new BigDecimal("0.1")) < 0 || interestRate.compareTo(new BigDecimal("0.5")) > 0) {
            throw new IllegalArgumentException("Interest rate must be between 0.1 and 0.5.");
        }
        if (numberOfInstallments != 6 && numberOfInstallments != 9 && numberOfInstallments != 12 && numberOfInstallments != 24) {
            throw new IllegalArgumentException("Number of installments must be 6, 9, 12, or 24.");
        }

        // 2. Customer and Credit Limit Check
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        BigDecimal totalLoanAmountWithInterest = amount.multiply(BigDecimal.ONE.add(interestRate));
        totalLoanAmountWithInterest = totalLoanAmountWithInterest.setScale(2, RoundingMode.HALF_UP);
        BigDecimal availableCreditLimit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());

        if (availableCreditLimit.compareTo(totalLoanAmountWithInterest) < 0) {
            throw new IllegalArgumentException(
                    "Customer does not have sufficient credit limit. Available: " +
                            availableCreditLimit.setScale(2, RoundingMode.HALF_UP).toPlainString() +
                            ", Required: " +
                            totalLoanAmountWithInterest.setScale(2, RoundingMode.HALF_UP).toPlainString()
            );
        }

        // 3. Create Loan Object
        Loan loan = new Loan();
        loan.setCustomer(customer);
        loan.setLoanAmount(amount);
        loan.setNumberOfInstallment(numberOfInstallments);
        loan.setCreateDate(LocalDate.now());
        loan.setPaid(false);

        BigDecimal installmentAmount = totalLoanAmountWithInterest.divide(new BigDecimal(numberOfInstallments), 2, RoundingMode.HALF_UP);
        LocalDate firstDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        for (int i = 0; i < numberOfInstallments; i++) {
            LoanInstallment installment = new LoanInstallment();
            installment.setAmount(installmentAmount);
            installment.setDueDate(firstDueDate.plusMonths(i));
            installment.setPaid(false);
            installment.setLoan(loan);
            loan.getInstallments().add(installment);
        }

        // 4. Update Customer's Used Credit Limit
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(totalLoanAmountWithInterest));
        customerRepository.save(customer);

        // 5. Save Loan (and its installments due to CascadeType.ALL)
        return loanRepository.save(loan);
    }

    /**
     * Lists loans for a given customer, performing authorization checks.
     * If the authenticated user has a 'CUSTOMER' role, they can only list their own loans.
     * 'ADMIN' users can list loans for any customer.
     *
     * @param customerId     The ID of the customer whose loans are to be listed.
     * @param authentication The Authentication object for the current user.
     * @return A list of Loan objects.
     */
    public List<Loan> listLoansByCustomer(Long customerId, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        if (authentication.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_CUSTOMER"))) {
            User loggedInUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found: " + username));
            Customer customerOfLoggedInUser = customerRepository.findByUser_Id(loggedInUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer record not found for the logged-in user."));
            if (!customerOfLoggedInUser.getId().equals(customerId)) {
                throw new AccessDeniedException("You are not authorized to view loans for this customer.");
            }
        }
        customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        return loanRepository.findByCustomer_Id(customerId);
    }

    /**
     * Lists all installments for a given loan, performing authorization checks.
     * If the authenticated user has a 'CUSTOMER' role, they can only list installments for their own loans.
     * 'ADMIN' users can list installments for any loan.
     *
     * @param loanId         The ID of the loan whose installments are to be listed.
     * @param authentication The Authentication object for the current user.
     * @return A list of LoanInstallment objects.
     */
    public List<LoanInstallment> listInstallmentsByLoan(Long loanId, Authentication authentication) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));

        if (authentication.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_CUSTOMER"))) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            User loggedInUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found: " + username));
            if (loan.getCustomer() == null || loan.getCustomer().getUser() == null ||
                    !loan.getCustomer().getUser().getId().equals(loggedInUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view installments for this loan.");
            }
        }
        return new ArrayList<>(loan.getInstallments());
    }

    /**
     * Processes a payment for a specified loan.
     * The payment can cover multiple installments if the amount is sufficient.
     * Installments are paid in full, starting with the earliest due.
     * Discounts for early payments and penalties for late payments are applied.
     * Payments are restricted to installments due within the next 3 calendar months.
     *
     * @param loanId         The ID of the loan for which the payment is made.
     * @param paymentAmount  The amount of money the customer intends to pay.
     * @param authentication The Authentication object for the current user.
     * @return A PayLoanResponse object summarizing the transaction.
     */
    @Transactional
    public PayLoanResponse payLoan(Long loanId, BigDecimal paymentAmount, Authentication authentication) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));

        if (authentication.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_CUSTOMER"))) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            User loggedInUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found: " + username));
            if (loan.getCustomer() == null || loan.getCustomer().getUser() == null ||
                    !loan.getCustomer().getUser().getId().equals(loggedInUser.getId())) {
                throw new AccessDeniedException("You are not authorized to make a payment for this loan.");
            }
        }

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be a positive value.");
        }
        if (loan.isPaid()) {
            return new PayLoanResponse(0, BigDecimal.ZERO, true, "This loan has already been fully paid.");
        }

        List<LoanInstallment> unpaidInstallments = loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(loanId);
        if (unpaidInstallments.isEmpty()) {
            return new PayLoanResponse(0, BigDecimal.ZERO, loan.isPaid(), "No unpaid installments found for this loan.");
        }

        BigDecimal remainingAmountToPay = paymentAmount;
        BigDecimal totalAmountActuallySpent = BigDecimal.ZERO;
        int installmentsPaidCount = 0;
        List<LoanInstallment> installmentsToUpdate = new ArrayList<>();
        String feedbackMessage = null;
        boolean discountAppliedThisTransaction = false; // Flag to track if a discount was applied in this transaction

        LocalDate paymentWindowEnd = LocalDate.now().plusMonths(3);

        for (LoanInstallment installment : unpaidInstallments) {
            if (installment.getDueDate().isAfter(paymentWindowEnd)) {
                if (installmentsPaidCount == 0) {
                    feedbackMessage = "No installments are currently payable within the 3-month payment window.";
                }
                break;
            }

            BigDecimal actualPaymentForInstallment = installment.getAmount();
            LocalDate today = LocalDate.now();
            LocalDate dueDate = installment.getDueDate();

            if (today.isBefore(dueDate)) {
                long daysBefore = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
                BigDecimal discount = installment.getAmount().multiply(new BigDecimal("0.001")).multiply(new BigDecimal(daysBefore));
                actualPaymentForInstallment = installment.getAmount().subtract(discount);
                if (installmentsPaidCount == 0) { // Only set discount message for the first relevant installment in this payment run
                    discountAppliedThisTransaction = true;
                }
            } else if (today.isAfter(dueDate)) {
                long daysAfter = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
                BigDecimal penalty = installment.getAmount().multiply(new BigDecimal("0.001")).multiply(new BigDecimal(daysAfter));
                actualPaymentForInstallment = installment.getAmount().add(penalty);
            }
            actualPaymentForInstallment = actualPaymentForInstallment.setScale(2, RoundingMode.HALF_UP);

            if (remainingAmountToPay.compareTo(actualPaymentForInstallment) >= 0) {
                installment.setPaid(true);
                installment.setPaymentDate(today);
                installment.setPaidAmount(actualPaymentForInstallment);
                installmentsToUpdate.add(installment);

                remainingAmountToPay = remainingAmountToPay.subtract(actualPaymentForInstallment);
                totalAmountActuallySpent = totalAmountActuallySpent.add(actualPaymentForInstallment);
                installmentsPaidCount++;
            } else {
                if (installmentsPaidCount == 0) {
                    if (paymentAmount.compareTo(installment.getAmount()) < 0 && paymentAmount.compareTo(actualPaymentForInstallment) < 0) {
                        feedbackMessage = "Payment amount is insufficient to cover the principal of the first due installment (Amount: " + installment.getAmount().setScale(2, RoundingMode.HALF_UP) + ").";
                    } else if (paymentAmount.compareTo(actualPaymentForInstallment) < 0) {
                        feedbackMessage = "Payment amount is insufficient to cover the first due installment including any applicable penalty/discount (Total Due: " + actualPaymentForInstallment.setScale(2, RoundingMode.HALF_UP) + ").";
                    } else {
                        feedbackMessage = "Payment amount is insufficient to cover the first due installment.";
                    }
                }
                break;
            }
        }

        boolean isLoanNowFullyPaid = loan.isPaid();
        if (installmentsPaidCount > 0) {
            loanInstallmentRepository.saveAll(installmentsToUpdate);
            Customer customer = loan.getCustomer();
            customer.setUsedCreditLimit(customer.getUsedCreditLimit().subtract(totalAmountActuallySpent));
            customerRepository.save(customer);

            if (loanInstallmentRepository.findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(loanId).isEmpty()) {
                loan.setPaid(true);
                loanRepository.save(loan);
                isLoanNowFullyPaid = true;
            }
        }

        // Determine final feedback message
        if (discountAppliedThisTransaction && installmentsPaidCount > 0) {
            feedbackMessage = "An early payment discount was applied to your payment.";
        }
        if (isLoanNowFullyPaid && !"An early payment discount was applied to your payment.".equals(feedbackMessage) ) { // Only override if no discount message was set
            if (feedbackMessage == null ) { // or if there was another failure message, that should stay
                feedbackMessage = "Loan has been fully paid.";
            } else if (!feedbackMessage.toLowerCase().contains("insufficient")) { // Don't override failure messages
                feedbackMessage = "Loan has been fully paid.";
            }
        }


        PayLoanResponse response = new PayLoanResponse(installmentsPaidCount, totalAmountActuallySpent, isLoanNowFullyPaid);
        if (feedbackMessage != null) {
            response.setMessage(feedbackMessage);
        }
        return response;
    }
}