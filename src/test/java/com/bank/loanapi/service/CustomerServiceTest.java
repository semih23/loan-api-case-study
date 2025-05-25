package com.bank.loanapi.service;

import com.bank.loanapi.model.Customer;
import com.bank.loanapi.model.User;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link CustomerService} class.
 * This class verifies the behavior of customer creation logic,
 * including input validation, username uniqueness, and interaction with repositories.
 */
@ExtendWith(MockitoExtension.class) // Integrates Mockito with JUnit 5.
class CustomerServiceTest {

    @Mock // Creates a mock instance of CustomerRepository.
    private CustomerRepository customerRepository;

    @Mock // Creates a mock instance of UserRepository.
    private UserRepository userRepository;

    @Mock // Creates a mock instance of PasswordEncoder.
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Creates an instance of CustomerService and injects the @Mock annotated fields.
    private CustomerService customerService;

    /**
     * Tests successful creation of a customer and their associated user account
     * when all input data is valid and the username is unique.
     */
    @Test
    void createCustomer_ShouldCreateCustomerAndUser_WhenDataIsValidAndUsernameIsUnique() {
        // Arrange: Define test data and mock behaviors.
        String name = "Ahmet";
        String surname = "Yıldız";
        BigDecimal creditLimit = new BigDecimal("50000.00");
        String username = "ahmetyildiz";
        String rawPassword = "Password123!";
        String hashedPassword = "hashedPassword123"; // Simulate the hashed version of the password.

        // Define mock behaviors:
        // 1. Simulate that the username is unique (repository returns an empty Optional).
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        // 2. Simulate the password encoder returning a specific hashed password.
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        // 3. When customerRepository.save is called, return the Customer object passed as an argument
        //    (or a version with an ID assigned).
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customerToSave = invocation.getArgument(0);
            // To closely mimic real behavior, we could set IDs here if they are generated upon save.
            // For simplicity in this test, returning the argument is often sufficient if we verify its state.
            // If ID generation needs to be tested:
            // customerToSave.setId(1L);
            // if (customerToSave.getUser() != null) customerToSave.getUser().setId(1L);
            return customerToSave;
        });

        // Act: Call the method under test.
        Customer createdCustomer = customerService.createCustomer(name, surname, creditLimit, username, rawPassword);

        // Assert: Verify the results.
        assertNotNull(createdCustomer, "Created customer should not be null.");
        assertEquals(name, createdCustomer.getName(), "Customer name should match.");
        assertEquals(surname, createdCustomer.getSurname(), "Customer surname should match.");
        assertEquals(0, creditLimit.compareTo(createdCustomer.getCreditLimit()), "Credit limit should match.");
        assertEquals(0, BigDecimal.ZERO.compareTo(createdCustomer.getUsedCreditLimit()), "Used credit limit should be zero for a new customer.");

        // Verify the associated User object.
        User createdUser = createdCustomer.getUser();
        assertNotNull(createdUser, "Associated user should not be null.");
        assertEquals(username, createdUser.getUsername(), "Username should match.");
        assertEquals(hashedPassword, createdUser.getPassword(), "Password should be the hashed version.");
        assertEquals("ROLE_CUSTOMER", createdUser.getRoles(), "User role should be ROLE_CUSTOMER.");

        // Verify that repository methods were called as expected.
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).encode(rawPassword);

        // Capture the Customer object passed to customerRepository.save and verify its details.
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(1)).save(customerCaptor.capture());

        Customer capturedCustomer = customerCaptor.getValue();
        assertNotNull(capturedCustomer.getUser(), "Captured customer should have a user linked.");
        assertEquals(username, capturedCustomer.getUser().getUsername());
        assertEquals(hashedPassword, capturedCustomer.getUser().getPassword());
        assertEquals("ROLE_CUSTOMER", capturedCustomer.getUser().getRoles());
    }

    /**
     * Tests that an IllegalArgumentException is thrown when attempting to create a customer
     * with a username that already exists in the system.
     */
    @Test
    void createCustomer_ShouldThrowException_WhenUsernameAlreadyExists() {
        // Arrange: Define an existing username and other necessary (but not directly tested) customer info.
        String existingUsername = "existingUser";
        String name = "Test";
        String surname = "User";
        BigDecimal creditLimit = new BigDecimal("10000");
        String password = "password";

        // Define mock behavior:
        // When userRepository.findByUsername is called with "existingUser",
        // return a non-empty Optional (indicating the user exists).
        // The content of the returned User object doesn't matter for this test, only its presence.
        when(userRepository.findByUsername(existingUsername)).thenReturn(Optional.of(new User()));

        // Act & Assert: Expect an IllegalArgumentException when createCustomer is called with the existing username.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer(name, surname, creditLimit, existingUsername, password);
        });

        // Verify that the message of the thrown exception is correct.
        assertEquals("Username already exists: " + existingUsername, exception.getMessage());

        // In this failure scenario, no save operations or password encoding should occur.
        verify(passwordEncoder, never()).encode(anyString()); // Password encoding should not be called.
        verify(customerRepository, never()).save(any(Customer.class)); // Customer should not be saved.
    }

    /**
     * Tests that an IllegalArgumentException is thrown when attempting to create a customer
     * with a blank first name.
     */
    @Test
    void createCustomer_ShouldThrowException_WhenNameIsBlank() {
        // Arrange: Set up test data with a blank name.
        String blankName = ""; // Blank name
        String surname = "Yılmaz";
        BigDecimal creditLimit = new BigDecimal("10000");
        String username = "testuser";
        String password = "password";

        // We don't expect userRepository.findByUsername to be called in this scenario,
        // as validation should throw an exception earlier.

        // Act & Assert: Expect an IllegalArgumentException.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer(blankName, surname, creditLimit, username, password);
        });

        assertEquals("Customer first name cannot be blank.", exception.getMessage());

        // In this input validation failure scenario, no repository or encoder methods should be called.
        verify(userRepository, never()).findByUsername(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(customerRepository, never()).save(any(Customer.class));
    }
}