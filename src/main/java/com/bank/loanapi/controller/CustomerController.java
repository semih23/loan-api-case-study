package com.bank.loanapi.controller;

import com.bank.loanapi.dto.CreateCustomerRequest;
import com.bank.loanapi.model.Customer;
import com.bank.loanapi.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing customer-related operations.
 * Base path for all customer endpoints is /api/v1/customers.
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Constructor for CustomerController.
     * Injects the CustomerService dependency for handling business logic.
     *
     * @param customerService The service responsible for customer operations.
     */
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * API endpoint to create a new customer along with their associated user account.
     * Expects customer details, username, and password in the request body.
     *
     * @param request The DTO containing data for the new customer and their user account.
     * @return A ResponseEntity containing the created Customer object and HTTP status 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Only users with the 'ADMIN' role can execute this method.
    public ResponseEntity<Customer> createCustomer(@RequestBody CreateCustomerRequest request) {
        Customer createdCustomer = customerService.createCustomer(
                request.getName(),
                request.getSurname(),
                request.getCreditLimit(),
                request.getUsername(),     // Username for the new user account
                request.getPassword()      // Password for the new user account
        );
        return new ResponseEntity<>(createdCustomer, HttpStatus.CREATED);
    }
}