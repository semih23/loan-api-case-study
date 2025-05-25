package com.bank.loanapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for carrying the response payload after a loan payment attempt.
 * This class defines the structure of the response body sent by the pay loan API endpoint.
 */
@Data // Lombok: Generates boilerplate code like getters, setters, toString, equals, and hashCode.
@NoArgsConstructor // Lombok: Generates a no-argument constructor.
@AllArgsConstructor // Lombok: Generates a constructor with all arguments for all fields.
// This annotation ensures that fields with null values are not included in the JSON output.
// This helps keep the API response cleaner, especially for the optional 'message' field.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayLoanResponse {

    /**
     * The number of installments that were successfully paid during this transaction.
     */
    private int installmentsPaid;

    /**
     * The total actual amount of money spent from the payment to cover the installments,
     * including any discounts for early payment or penalties for late payment.
     */
    private BigDecimal totalAmountSpent;

    /**
     * A boolean flag indicating whether the entire loan (all its installments) has been
     * completely paid off as a result of this payment.
     */
    private boolean isLoanPaidCompletely;

    /**
     * An optional message providing feedback about the payment transaction.
     * This can include information about discounts applied, reasons for payment failure (e.g., insufficient funds due to penalty),
     * or confirmation of full loan payment. It will only be included in the JSON response if it's not null.
     */
    private String message;

    /**
     * Convenience constructor for creating a PayLoanResponse without a specific message.
     * The 'message' field will be null and thus excluded from JSON output by default.
     *
     * @param installmentsPaid      The number of installments paid.
     * @param totalAmountSpent      The total amount spent in the transaction.
     * @param isLoanPaidCompletely  Whether the loan is now fully paid.
     */
    public PayLoanResponse(int installmentsPaid, BigDecimal totalAmountSpent, boolean isLoanPaidCompletely) {
        this.installmentsPaid = installmentsPaid;
        this.totalAmountSpent = totalAmountSpent;
        this.isLoanPaidCompletely = isLoanPaidCompletely;
        // The 'message' field remains null by default with this constructor.
    }
}