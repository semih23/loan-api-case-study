package com.bank.loanapi.service;

import com.bank.loanapi.model.User;
import com.bank.loanapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
// import java.util.stream.Collectors; // Not strictly needed if Set.of is used and no other stream operations

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link CustomUserDetailsService} class.
 * Verifies the behavior of loading user details for Spring Security.
 */
@ExtendWith(MockitoExtension.class) // Integrates Mockito with JUnit 5 for mock creation and injection.
class CustomUserDetailsServiceTest {

    @Mock // Creates a mock instance of UserRepository.
    private UserRepository userRepository;

    @InjectMocks // Creates an instance of CustomUserDetailsService and injects the mocked UserRepository.
    private CustomUserDetailsService customUserDetailsService;

    /**
     * Tests the loadUserByUsername method for a scenario where the user exists.
     * Verifies that the correct UserDetails object is returned with accurate information.
     */
    @Test
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        // Arrange: Define test data and mock behavior.
        String testUsername = "testuser";
        String testPassword = "hashedPassword"; // Simulate a hashed password from the database.
        String testRoles = "ROLE_USER,ROLE_VIEWER";

        User expectedUser = new User(testUsername, testPassword, testRoles);
        expectedUser.setId(1L); // Optionally set an ID for completeness.

        // Configure the mock userRepository to return the expectedUser when findByUsername is called.
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(expectedUser));

        // Act: Call the service method to load the user.
        UserDetails actualUserDetails = customUserDetailsService.loadUserByUsername(testUsername);

        // Assert: Verify the properties of the returned UserDetails object.
        assertNotNull(actualUserDetails, "Returned UserDetails should not be null.");
        assertEquals(testUsername, actualUserDetails.getUsername(), "Username should match.");
        assertEquals(testPassword, actualUserDetails.getPassword(), "Password should match.");

        // Verify authorities (roles).
        Collection<? extends GrantedAuthority> expectedAuthorities =
                Set.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_VIEWER"));

        assertNotNull(actualUserDetails.getAuthorities(), "Authorities should not be null.");
        assertEquals(expectedAuthorities.size(), actualUserDetails.getAuthorities().size(), "Number of authorities should match.");
        // Verify that both collections contain the same authorities, regardless of order.
        assertTrue(actualUserDetails.getAuthorities().containsAll(expectedAuthorities) &&
                        expectedAuthorities.containsAll(actualUserDetails.getAuthorities()),
                "Authorities should match the expected roles.");

        // Verify the default account status flags from our User entity (which implements UserDetails).
        assertTrue(actualUserDetails.isAccountNonExpired(), "Account should be non-expired by default.");
        assertTrue(actualUserDetails.isAccountNonLocked(), "Account should be non-locked by default.");
        assertTrue(actualUserDetails.isCredentialsNonExpired(), "Credentials should be non-expired by default.");
        assertTrue(actualUserDetails.isEnabled(), "Account should be enabled by default.");

        // Verify that userRepository.findByUsername was called exactly once with the correct username.
        verify(userRepository, times(1)).findByUsername(testUsername);
    }

    /**
     * Tests the loadUserByUsername method for a scenario where the user does not exist.
     * Verifies that a UsernameNotFoundException is thrown with the appropriate message.
     */
    @Test
    void loadUserByUsername_WhenUserDoesNotExist_ShouldThrowUsernameNotFoundException() {
        // Arrange: Define a username that does not exist in the system.
        String nonExistentUsername = "unknownuser";

        // Configure the mock userRepository to return an empty Optional, simulating user not found.
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // Act & Assert: Verify that UsernameNotFoundException is thrown.
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(nonExistentUsername);
        });

        // Verify that the exception message is correct.
        assertEquals("User not found with username: " + nonExistentUsername, exception.getMessage());

        // Verify that userRepository.findByUsername was called exactly once with the correct username.
        verify(userRepository, times(1)).findByUsername(nonExistentUsername);
    }
}