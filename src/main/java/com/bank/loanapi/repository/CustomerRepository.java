package com.bank.loanapi.repository;

import com.bank.loanapi.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Indicates that this interface is a Spring Bean and will be used for database operations.
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // By extending JpaRepository<EntityType, ID_Type>,
    // basic CRUD operations (save, findById, findAll, delete, etc.) for the Customer entity
    // are automatically provided by Spring Data JPA.
    // No need to add custom methods here unless specific query logic is required.

    /**
     * Finds a Customer by the ID of their associated User.
     * Spring Data JPA automatically implements this method based on its naming convention.
     * It looks for a 'user' property in the Customer entity, and then for an 'id' property within that 'user' object.
     *
     * @param userId The ID of the User associated with the Customer.
     * @return An Optional containing the Customer if found, or an empty Optional otherwise.
     */
    Optional<Customer> findByUser_Id(Long userId);
}