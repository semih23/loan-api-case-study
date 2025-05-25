package com.bank.loanapi.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for carrying the payload when making a loan payment.
 * This class defines the structure of the request body expected by the pay loan API endpoint.
 */
@Data // Lombok: Generates boilerplate code like getters, setters, toString, equals, and hashCode.
public class PayLoanRequest {

    /**
     * The amount of money the customer wishes to pay towards the loan.
     * This amount will be used to pay off one or more installments based on the business logic
     * (e.g., paying the earliest due installments first, handling early/late payment adjustments).
     */
    private BigDecimal amount;
}