package com.bank.loanapi.repository;

import com.bank.loanapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Indicates that this interface is a Spring Bean and will be used for database operations.
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a User by their username.
     * This method is essential for Spring Security to load user details during the authentication process.
     * Spring Data JPA automatically implements this method based on its naming convention.
     *
     * @param username The username of the User to find.
     * @return An Optional containing the User if found, or an empty Optional otherwise.
     */
    Optional<User> findByUsername(String username);
}