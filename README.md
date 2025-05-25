# Credit Module API

This project is a Spring Boot-based REST API designed for a bank, enabling employees and customers (with appropriate permissions) to manage loan creation, listing, and payments. The system features role-based access control for ADMIN and CUSTOMER users and includes logic for early/late payment adjustments.

---

## Requirements

To build and run this project, you need to have the following installed on your machine:
* **Java Development Kit (JDK)** - Version 21 or higher
* **Apache Maven** - Version 3.8 or higher

---

## How to Build and Run the Project

1.  **Build the Project:**
    In the project's root directory, open a terminal and run the following Maven command. This will compile the project, run available tests, and create an executable `.jar` file in the `target` directory.
    ```bash
    mvn clean install
    ```

2.  **Run the Project:**
    After the build is successfully completed, you can start the application with the following command:
    ```bash
    java -jar target/loan-api-0.0.1-SNAPSHOT.jar
    ```
    The application will start on the default port `8080`. Upon first run with an empty database, a default `admin` user will be created.

---

## API Usage

### Authentication

All API endpoints are secured using **HTTP Basic Authentication**. Every request must include valid credentials in the `Authorization` header.

**User Roles and Credentials:**

* **ADMIN Role:**
    * Has full access to all API endpoints and can operate on behalf of any customer.
    * A default admin user is created on first application startup if no users exist in the database:
        * **Username:** `admin`
        * **Password:** `1234`
* **CUSTOMER Role:**
    * Can perform operations only related to their own data (e.g., list their own loans, pay their own loans).
    * Customer users and their credentials are created by an ADMIN via the "Create Customer" endpoint.

When using a tool like Postman, select the "Basic Auth" type in the "Authorization" tab and enter the appropriate credentials.

### API Endpoints

#### 1. Create Customer (and Associated User Account)

* **Endpoint:** `POST /api/v1/customers`
* **Description:** Creates a new customer and their associated user login credentials. This action can only be performed by an ADMIN.
* **Request Body:**
    ```json
    {
        "name": "Zeynep",
        "surname": "Demir",
        "creditLimit": 60000,
        "username": "zeynepd",
        "password": "Password123!"
    }
    ```
* **Security:** `ADMIN` role required.

#### 2. Create Loan

* **Endpoint:** `POST /api/v1/loans`
* **Description:** Creates a new loan and its installments for a given customer. The system checks if the customer has enough credit limit. Number of installments can be 6, 9, 12, or 24. Interest rate must be between 0.1 and 0.5. Total loan amount is calculated as `principalAmount * (1 + interestRate)`. Installment due dates are the first day of subsequent months.
* **Request Body:**
    ```json
    {
        "customerId": 1,
        "amount": 20000,
        "interestRate": 0.20,
        "numberOfInstallments": 12
    }
    ```
* **Security:** `ADMIN` role required.

#### 3. List Loans for Customer

* **Endpoint:** `GET /api/v1/loans?customerId={id}`
* **Description:** Lists all loans for the customer with the specified `customerId`.
    * An `ADMIN` user can list loans for any `customerId`.
    * A `CUSTOMER` user can only list their own loans (the `customerId` in the request must match their own).
* **Example Request:** `GET http://localhost:8080/api/v1/loans?customerId=1`
* **Security:** `ADMIN` or `CUSTOMER` (own data) role required.

#### 4. List Installments for Loan

* **Endpoint:** `GET /api/v1/loans/{loanId}/installments`
* **Description:** Lists all installments for the loan with the specified `loanId`.
    * An `ADMIN` user can list installments for any `loanId`.
    * A `CUSTOMER` user can only list installments for their own loans.
* **Example Request:** `GET http://localhost:8080/api/v1/loans/1/installments`
* **Security:** `ADMIN` or `CUSTOMER` (own data) role required.

#### 5. Pay Loan

* **Endpoint:** `POST /api/v1/loans/{loanId}/pay`
* **Description:** Makes a payment for the loan with the specified `loanId`. The endpoint can pay multiple installments if the sent amount is sufficient. Installments are paid wholly. The earliest due installment is paid first. Installments with a due date more than 3 calendar months away cannot be paid.
    * Discounts are applied for early payments, and penalties for late payments (0.1% of installment amount per day difference).
    * An `ADMIN` user can pay any loan.
    * A `CUSTOMER` user can only pay their own loans.
* **Request Body:**
    ```json
    {
        "amount": 2500
    }
    ```
* **Example Success Response (with early payment discount):**
    ```json
    {
        "installmentsPaid": 1,
        "totalAmountSpent": 990.00,
        "isLoanPaidCompletely": false,
        "message": "An early payment discount was applied to your payment." 
    }
    ```
* **Example Failure Response (insufficient for penalized amount):**
    ```json
    {
        "installmentsPaid": 0,
        "totalAmountSpent": 0.00,
        "isLoanPaidCompletely": false,
        "message": "Payment amount is insufficient to cover the first due installment including any applicable penalty/discount (Total Due: 1010.00 TL)."
    }
    ```
* **Security:** `ADMIN` or `CUSTOMER` (own data) role required.

---