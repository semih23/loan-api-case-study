package com.bank.loanapi.service;

import com.bank.loanapi.model.Customer;
import com.bank.loanapi.model.User;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service class for handling customer-related business logic.
 * This includes creating new customers and their associated user accounts.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor for CustomerService.
     * Injects dependencies for customer and user repositories, and the password encoder.
     *
     * @param customerRepository Repository for customer data operations.
     * @param userRepository     Repository for user data operations.
     * @param passwordEncoder    Encoder for hashing user passwords.
     */
    public CustomerService(CustomerRepository customerRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new customer and their associated user account.
     * Performs validation on input data and ensures username uniqueness.
     * The new user is assigned the 'ROLE_CUSTOMER' by default.
     *
     * @param name        The first name of the customer.
     * @param surname     The last name (surname) of the customer.
     * @param creditLimit The credit limit for the customer.
     * @param username    The desired username for the new user account.
     * @param password    The raw password for the new user account (will be encoded before saving).
     * @return The created Customer object.
     * @throws IllegalArgumentException if input data is invalid or if the username already exists.
     */
    @Transactional // Ensures that all database operations within this method are part of a single transaction.
    public Customer createCustomer(String name, String surname, BigDecimal creditLimit, String username, String password) {
        // Input validations
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer first name cannot be blank.");
        }
        if (surname == null || surname.isBlank()) {
            throw new IllegalArgumentException("Customer surname cannot be blank.");
        }
        if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit limit cannot be null or negative.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }

        // Check if the username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        // Create a new User object
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // Always encode the password before saving!
        newUser.setRoles("ROLE_CUSTOMER"); // Default role for new customers

        // Create a new Customer object
        Customer newCustomer = new Customer();
        newCustomer.setName(name);
        newCustomer.setSurname(surname);
        newCustomer.setCreditLimit(creditLimit);
        newCustomer.setUsedCreditLimit(BigDecimal.ZERO); // New customers start with zero used credit.

        // Link the Customer and User
        newCustomer.setUser(newUser);
        // If the User entity also has a reference to Customer and it's the inverse side (mappedBy),
        // JPA handles the association when the owning side (Customer) is saved with CascadeType.ALL.
        // newUser.setCustomer(newCustomer); // This line might be needed if User owns the relationship or for bidirectional consistency before save.
        // However, with Customer owning the @OneToOne with Cascade.ALL, saving customer will save user.

        // Save the Customer (which will also save the associated User due to CascadeType.ALL on the Customer.user field)
        return customerRepository.save(newCustomer);
    }
}