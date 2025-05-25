package com.bank.loanapi.repository;

import com.bank.loanapi.model.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Import for the commented-out example method
import java.util.List;

@Repository // Indicates that this interface is a Spring Bean and will be used for database operations.
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {

    /**
     * Finds all LoanInstallment entities associated with a specific loan ID.
     * Spring Data JPA automatically implements this method based on its naming convention.
     * It looks for a 'loan' property in the LoanInstallment entity, and then for an 'id' property within that 'loan' object.
     *
     * @param loanId The ID of the Loan.
     * @return A list of LoanInstallment entities associated with the given loanId.
     */
    List<LoanInstallment> findByLoan_Id(Long loanId);

    /**
     * Finds all unpaid (isPaid=false) LoanInstallment entities for a specific loan ID,
     * ordered by their due date in ascending order (earliest due date first).
     * This method is crucial for processing payments in the correct order.
     *
     * @param loanId The ID of the Loan.
     * @return A list of unpaid LoanInstallment entities, sorted by due date.
     */
    List<LoanInstallment> findByLoan_IdAndIsPaidFalseOrderByDueDateAsc(Long loanId);

    // Example of a more complex query method that could be needed for advanced payment processing:
    // This method would find unpaid installments for a specific loan, due on or before a certain date, ordered by due date.
    // For now, only the basic listing and the specific payment processing query above are implemented.
    // Example:
    // List<LoanInstallment> findByLoan_IdAndIsPaidFalseAndDueDateLessThanEqualOrderByDueDateAsc(Long loanId, LocalDate date);
}