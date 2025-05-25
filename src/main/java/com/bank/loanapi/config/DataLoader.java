package com.bank.loanapi.config;

import com.bank.loanapi.model.User;
import com.bank.loanapi.repository.CustomerRepository; // Kept for potential future use or if other initial data logic depends on it
import com.bank.loanapi.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// import java.math.BigDecimal; // No longer needed if we don't create sample Customers here

/**
 * Component responsible for loading initial data into the database when the application starts.
 * This is particularly useful for creating a default admin user if no users exist,
 * ensuring the application is accessible immediately after a fresh deployment.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor for DataLoader.
     * Injects necessary repositories and the password encoder.
     *
     * @param userRepository     Repository for user data.
     * @param passwordEncoder    Encoder for hashing passwords.
     */
    public DataLoader(
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Callback method executed by Spring Boot after the application context is loaded.
     * This method checks if any users exist in the database. If not, it creates a default admin user.
     *
     * @param args Incoming command line arguments.
     * @throws Exception if an error occurs during data loading.
     */
    @Override
    public void run(String... args) throws Exception {
        // If no users exist in the database, create a default admin user.
        if (userRepository.count() == 0) {
            System.out.println("No users found in the database. Creating default admin user...");
            User adminUser = new User(
                    "admin",
                    passwordEncoder.encode("1234"), // Encode the password!
                    "ROLE_ADMIN" // Assign the ADMIN role
            );
            userRepository.save(adminUser);
            System.out.println("Default admin user created successfully.");
        } else {
            System.out.println("Users already exist in the database. Default admin user not created.");
        }


    }
}