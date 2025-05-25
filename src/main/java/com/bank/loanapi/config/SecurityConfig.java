package com.bank.loanapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// Imports for InMemoryUserDetailsManager, User, and UserDetails are no longer needed
// as we are using a custom UserDetailsService.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Configuration class for Spring Security.
 * This class defines how the application's security is handled, including
 * password encoding, user details service (implicitly via CustomUserDetailsService),
 * and HTTP security rules (e.g., which paths are public, which require authentication).
 */
@Configuration
@EnableWebSecurity // Enables Spring Security's web security support.
@EnableMethodSecurity // Enables method-level security annotations like @PreAuthorize.
public class SecurityConfig {

    /**
     * Defines the PasswordEncoder bean that will be used for encoding and verifying passwords.
     * BCryptPasswordEncoder is a strong hashing algorithm recommended for password storage.
     *
     * @return A PasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // The UserDetailsService bean (previously InMemoryUserDetailsManager) has been REMOVED.
    // Spring Boot will automatically detect and use our CustomUserDetailsService @Service bean
    // because it implements the UserDetailsService interface.

    /**
     * Defines the SecurityFilterChain bean that configures HTTP security rules.
     * This method specifies how requests are authorized, which authentication methods are used,
     * and other security-related configurations like CSRF and frame options.
     *
     * @param http The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (Cross-Site Request Forgery) protection.
                // While important for stateful, browser-based applications,
                // it's commonly disabled for stateless REST APIs.
                .csrf(csrf -> csrf.disable())

                // Configure frame options to allow the H2 console (which runs in a frame)
                // to be displayed when served from the same origin.
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )

                // Configure authorization rules for HTTP requests.
                .authorizeHttpRequests(auth -> auth
                        // Allow all requests to the H2 console path without authentication.
                        .requestMatchers("/h2-console/**").permitAll()
                        // All other requests must be authenticated.
                        .anyRequest().authenticated()
                )
                // Enable HTTP Basic Authentication as the authentication method.
                .httpBasic(withDefaults());
        return http.build();
    }
}