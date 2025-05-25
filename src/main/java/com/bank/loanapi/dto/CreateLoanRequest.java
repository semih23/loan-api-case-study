package com.bank.loanapi.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for carrying the payload when creating a new loan.
 * This class defines the structure of the request body expected by the create loan API endpoint.
 */
@Data // Lombok: Generates boilerplate code like getters, setters, toString, equals, and hashCode.
public class CreateLoanRequest {

    /**
     * The ID of the customer for whom the loan is being created.
     */
    private Long customerId;

    /**
     * The principal amount of the loan being requested.
     * This is the base amount before any interest is applied.
     */
    private BigDecimal amount;

    /**
     * The interest rate for the loan.
     * This should be a decimal value (e.g., 0.15 for 15%).
     * As per requirements, this rate must be between 0.1 and 0.5.
     */
    private BigDecimal interestRate;

    /**
     * The number of installments for the loan.
     * As per requirements, this can only be 6, 9, 12, or 24.
     */
    private int numberOfInstallments;
}