package com.bank.loanapi.repository;

import com.bank.loanapi.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Indicates that this interface is a Spring Bean and will be used for database operations.
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /**
     * Finds all Loan entities associated with a specific customer ID.
     * Spring Data JPA automatically implements this method based on its naming convention.
     * It looks for a 'customer' property in the Loan entity, and then for an 'id' property within that 'customer' object.
     *
     * @param customerId The ID of the Customer.
     * @return A list of Loan entities associated with the given customerId.
     */
    List<Loan> findByCustomer_Id(Long customerId);
}