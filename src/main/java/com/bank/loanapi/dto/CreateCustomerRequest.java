package com.bank.loanapi.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for carrying the payload when creating a new customer.
 * This class defines the structure of the request body expected by the create customer API endpoint.
 */
@Data // Lombok: Generates boilerplate code like getters, setters, toString, equals, and hashCode.
public class CreateCustomerRequest {

    /**
     * The first name of the customer.
     */
    private String name;

    /**
     * The last name (surname) of the customer.
     */
    private String surname;

    /**
     * The credit limit assigned to the customer.
     */
    private BigDecimal creditLimit;

    /**
     * The desired username for the customer's associated user account.
     * This will be used for logging into the system.
     */
    private String username;

    /**
     * The desired password for the customer's associated user account.
     * This should be a raw, unencrypted password provided by the client;
     * it will be securely hashed by the server before storage.
     */
    private String password;
}