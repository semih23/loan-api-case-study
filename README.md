# Credit Module API

This project is a Spring Boot-based REST API designed for a bank, enabling authorized employees (ADMIN role) and customers (CUSTOMER role) to manage loan creation, listing, and payments. The system features role-based access control and includes logic for early/late payment adjustments. It aims to simulate a production-ready application.

---

## Requirements

To build and run this project, you need to have the following installed on your machine:
* **Java Development Kit (JDK)** - Version 21 or higher
* **Apache Maven** - Version 3.8 or higher

---

## How to Build and Run the Project

1.  **Build the Project:**
    In the project's root directory, open a terminal and run the following Maven command. This will compile the project, run available unit tests, and create an executable `.jar` file in the `target` directory.
    ```bash
    mvn clean install
    ```

2.  **Run the Project:**
    After the build is successfully completed, you can start the application with the following command:
    ```bash
    java -jar target/LoanAPI-0.0.1-SNAPSHOT.jar
    ```
    The application will start on the default port `8080`. Upon first run with an empty database, a default `admin` user will be created (username: `admin`, password: `1234`).

---

## Database Schema Overview

All primary business information is stored in the database using the H2 database engine. The core entities are:

* **Customer**:
    * `id`: (Primary Key, Auto-generated)
    * `name`: (String, Not Null)
    * `surname`: (String, Not Null)
    * `creditLimit`: (BigDecimal, Not Null)
    * `usedCreditLimit`: (BigDecimal, Not Null)
    * `user_id`: (Foreign Key to `app_users` table, for linking to the user account)
* **Loan**:
    * `id`: (Primary Key, Auto-generated)
    * `customer_id`: (Foreign Key to `customers` table)
    * `loanAmount`: (BigDecimal, Not Null) - The principal amount.
    * `numberOfInstallment`: (int, Not Null)
    * `createDate`: (LocalDate, Not Null)
    * `isPaid`: (boolean, Not Null, defaults to false)
* **LoanInstallment**:
    * `id`: (Primary Key, Auto-generated)
    * `loan_id`: (Foreign Key to `loans` table)
    * `amount`: (BigDecimal, Not Null) - Original calculated amount for the installment.
    * `paidAmount`: (BigDecimal) - Actual amount paid, may differ from `amount` due to discounts/penalties.
    * `dueDate`: (LocalDate, Not Null)
    * `paymentDate`: (LocalDate) - Date when the installment was paid.
    * `isPaid`: (boolean, Not Null, defaults to false)
* **app_users** (Custom table for authentication):
    * `id`: (Primary Key, Auto-generated)
    * `username`: (String, Not Null, Unique)
    * `password`: (String, Not Null, Encoded)
    * `roles`: (String, e.g., "ROLE_ADMIN", "ROLE_CUSTOMER")

---

## Key Business Rules

### Loan Creation
* A customer must have sufficient available credit limit to obtain a new loan.
* The number of installments can only be 6, 9, 12, or 24.
* The interest rate must be between 0.1 (10%) and 0.5 (50%).
* All installments for a single loan must have the same calculated amount.
* The total amount to be repaid for a loan is calculated as: `principalLoanAmount * (1 + interestRate)`.
* The due date for installments is always the first day of the month.
* The first installment's due date is the first day of the month immediately following the loan creation month.

### Loan Payment
* Installments must be paid wholly; partial payments for a single installment are not allowed.
    * Example: If an installment is 10 units, sending 20 units pays 2 installments (if available and rules apply).
    * Sending 15 units pays 1 installment.
    * Sending 5 units pays no installments.
* The installment with the earliest due date must be paid first. If more money is available, payment continues to the next earliest due installment.
* Installments with a due date more than 3 calendar months from the current date cannot be paid.
    * Example: If the current month is January, only installments due in January, February, or March can be paid.
* The payment result will indicate how many installments were paid, the total amount spent, and if the loan is now completely paid off.
* Necessary updates are made to `customer` (usedCreditLimit), `loan` (isPaid), and `loanInstallment` (isPaid, paidAmount, paymentDate) tables upon payment.

### Bonus: Early/Late Payment Adjustments
* **Early Payment Discount:** If an installment is paid before its due date, a discount is applied.
    * Discount Formula: `installmentAmount * 0.001 * (number of days before due date)`.
    * In this case, the `paidAmount` for the installment will be lower than its original `amount`.
* **Late Payment Penalty:** If an installment is paid after its due date, a penalty is added.
    * Penalty Formula: `installmentAmount * 0.001 * (number of days after due date)`.
    * In this case, the `paidAmount` for the installment will be higher than its original `amount`.

---
## Unit Tests

22 unit tests have been written for all service layer components to ensure business logic correctness and to facilitate a production-like development approach. These tests cover various scenarios including successful operations, error conditions, and business rule validations. Tests can be run using the standard Maven command: `mvn test`.

---
## API Usage

### Authentication

All API endpoints are secured using **HTTP Basic Authentication**. Every request must include valid credentials in the `Authorization` header.

**User Roles and Credentials:**

* **ADMIN Role:**
    * Has full access to all API endpoints and can operate for all customers.
    * A default admin user is created on first application startup if no users exist in the database:
        * **Username:** `admin`
        * **Password:** `password` (This is the raw password; it will be encoded in the database).
* **CUSTOMER Role:**
    * Can perform operations only related to their own data (e.g., list their own loans, pay their own loans).
    * Customer users and their credentials are created by an ADMIN via the "Create Customer" endpoint. The username and password provided during customer creation are used for login.

When using a tool like Postman, select the "Basic Auth" type in the "Authorization" tab and enter the appropriate credentials.

### API Endpoints

#### 1. Create Customer (and Associated User Account)

* **Endpoint:** `POST /api/v1/customers`
* **Description:** Creates a new customer and their associated user login credentials.
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
* **Description:** Creates a new loan and its installments for a given customer. Adheres to all loan creation business rules (credit limit, installment count, interest rate, total amount calculation, due dates).
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
    * A `CUSTOMER` user can only list their own loans.
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
* **Description:** Makes a payment for the loan with the specified `loanId`. Adheres to all payment business rules (whole installments, earliest first, 3-month window, early/late payment adjustments).
    * An `ADMIN` user can pay any loan.
    * A `CUSTOMER` user can only pay their own loans.
* **Request Body:**
    ```json
    {
        "amount": 2500
    }
    ```
* **Example Success Response (with early payment discount):**
  The response indicates the number of installments paid, total amount spent (reflecting adjustments), and if the loan is fully paid.
    ```json
    {
        "installmentsPaid": 1,
        "totalAmountSpent": 990.00,
        "isLoanPaidCompletely": false,
        "message": "An early payment discount was applied to your payment."
    }
    ```
* **Security:** `ADMIN` or `CUSTOMER` (own data) role required.

---

