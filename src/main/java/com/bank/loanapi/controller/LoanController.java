package com.bank.loanapi.controller;

import com.bank.loanapi.dto.CreateLoanRequest;
import com.bank.loanapi.dto.PayLoanRequest;
import com.bank.loanapi.dto.PayLoanResponse;
import com.bank.loanapi.model.Loan;
import com.bank.loanapi.model.LoanInstallment;
import com.bank.loanapi.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing loan-related operations.
 * Base path for all loan endpoints is /api/v1/loans.
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    /**
     * Constructor for LoanController.
     * Injects the LoanService dependency using constructor injection.
     *
     * @param loanService The service responsible for loan-related business logic.
     */
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * API endpoint to create a new loan for a customer.
     * The request body should contain customer ID, loan amount, interest rate, and number of installments.
     *
     * @param request The DTO containing data for the new loan.
     * @return A ResponseEntity containing the created Loan object and HTTP status 201 (Created).
     */
    @PostMapping // Maps HTTP POST requests to "/api/v1/loans".
    @PreAuthorize("hasRole('ADMIN')") // Only users with the 'ADMIN' role can create loans.
    public ResponseEntity<Loan> createLoan(@RequestBody CreateLoanRequest request) {
        // Delegates the loan creation logic to the LoanService.
        Loan createdLoan = loanService.createLoan(
                request.getCustomerId(),
                request.getAmount(),
                request.getInterestRate(),
                request.getNumberOfInstallments()
        );
        // Returns the created loan object with HTTP status 201 (Created) upon successful creation.
        return new ResponseEntity<>(createdLoan, HttpStatus.CREATED);
    }

    /**
     * API endpoint to list loans for a specific customer.
     *
     * @param customerId     The ID of the customer whose loans are to be listed (passed as a request parameter).
     * @param authentication The authentication object for the currently logged-in user, used for authorization checks.
     * @return A ResponseEntity containing a list of Loan objects and HTTP status 200 (OK).
     */
    @GetMapping // Maps HTTP GET requests to "/api/v1/loans".
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')") // ADMINs or CUSTOMERs (for their own data) can access.
    public ResponseEntity<List<Loan>> listLoans(@RequestParam Long customerId, Authentication authentication) {
        List<Loan> loans = loanService.listLoansByCustomer(customerId, authentication);
        return ResponseEntity.ok(loans);
    }

    /**
     * API endpoint to list installments for a specific loan.
     *
     * @param loanId         The ID of the loan whose installments are to be listed (passed as a path variable).
     * @param authentication The authentication object for the currently logged-in user, used for authorization checks.
     * @return A ResponseEntity containing a list of LoanInstallment objects and HTTP status 200 (OK).
     */
    @GetMapping("/{loanId}/installments") // Maps HTTP GET requests to "/api/v1/loans/{loanId}/installments".
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')") // ADMINs or CUSTOMERs (for their own loan data) can access.
    public ResponseEntity<List<LoanInstallment>> listInstallments(@PathVariable Long loanId, Authentication authentication) {
        List<LoanInstallment> installments = loanService.listInstallmentsByLoan(loanId, authentication);
        return ResponseEntity.ok(installments);
    }

    /**
     * API endpoint to make a payment towards a specific loan.
     * The request body should contain the payment amount.
     *
     * @param loanId         The ID of the loan to be paid (passed as a path variable).
     * @param request        The DTO containing the payment amount.
     * @param authentication The authentication object for the currently logged-in user, used for authorization checks.
     * @return A ResponseEntity containing a PayLoanResponse DTO with payment summary and HTTP status 200 (OK).
     */
    @PostMapping("/{loanId}/pay") // Maps HTTP POST requests to "/api/v1/loans/{loanId}/pay".
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')") // ADMINs or CUSTOMERs (for their own loans) can access.
    public ResponseEntity<PayLoanResponse> payLoan(
            @PathVariable Long loanId,
            @RequestBody PayLoanRequest request,
            Authentication authentication) {

        PayLoanResponse response = loanService.payLoan(loanId, request.getAmount(), authentication);
        return ResponseEntity.ok(response);
    }
}