package com.bank.loanapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Loan API.
 * This class serves as the entry point for the Spring Boot application.
 */
@SpringBootApplication // This is a convenience annotation that adds all of the following:
// @Configuration: Tags the class as a source of bean definitions for the application context.
// @EnableAutoConfiguration: Tells Spring Boot to start adding beans based on classpath settings, other beans, and various property settings.
// @ComponentScan: Tells Spring to look for other components, configurations, and services in the 'com.bank.loanapi' package, allowing it to find controllers, services, etc.
public class LoanApiApplication {

    /**
     * The main method which serves as the entry point for the Spring Boot application.
     * It delegates to Spring Boot's SpringApplication class to bootstrap the application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(LoanApiApplication.class, args);
    }

}