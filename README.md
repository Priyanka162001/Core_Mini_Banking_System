# 🏦 Mini Core Banking System

## 📌 Overview

The **Mini Core Banking System** is a backend application built using Spring Boot that simulates real-world banking operations. It includes user management, savings accounts, transactions, KYC processing, and interest posting.

This project demonstrates clean architecture, secure API design, database versioning, and high test coverage.

## ⚙️ Tech Stack

* Java
* Spring Boot
* Spring Security (JWT Authentication)
* Hibernate / JPA
* PostgreSQL
* Flyway (Database Migration)
* Maven
* JUnit & Mockito
* JaCoCo (Code Coverage)


## 🚀 Features

### 👤 User & Authentication

* User Registration & Login
* JWT-based Authentication
* Role-based Authorization (Admin / Customer)

### 🧾 Customer Management

* Customer Profile Creation & Update
* Address Management
* KYC Verification Workflow

### 💰 Banking Operations

* Savings Account Creation
* Deposit & Withdrawal Transactions
* Transaction History & Summary

### 📈 Interest Management

* Interest Calculation
* Scheduled Interest Posting
* Interest History Tracking

### 🛡️ Additional Features

* Global Exception Handling
* Input Validation
* Idempotency Handling
* Secure REST APIs

---

## 🗂️ Project Structure

```
controller  → REST APIs  
service     → Business Logic  
repository  → Database Access  
entity      → Database Models  
dto         → Request/Response Objects  
config      → Security & Configuration  
scheduler   → Background Jobs  
```

---

## 🗄️ Database Configuration (PostgreSQL)

The application uses **PostgreSQL** as the primary database.

### ⚙️ Setup

1. Install PostgreSQL

2. Create database:

   ```sql
   CREATE DATABASE mini_banking;
   ```

3. Update `application.properties`:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/mini_banking
spring.datasource.username=postgres
spring.datasource.password=your_password
```

---

## 🔄 Database Migrations (Flyway)

* Flyway is used for version-controlled database schema management
* Migration scripts are located at:

```
src/main/resources/db/migration
```

### 📂 Migration Files

* V1__create_users.sql
* V2__create_savings_products.sql
* V3__create_savings_accounts.sql
* V4__create_transactions.sql
* V5__create_account_requests.sql
* V6__create_customer_profiles_and_addresses.sql
* V7__create_kyc_records.sql
* V8__create_interest_posting_records.sql
* V9__create_idempotency_keys.sql
* V10__add_rejection_reason_to_kyc.sql

### 🔗 View Scripts on GitHub

https://github.com/Priyanka162001/Core_Mini_Banking_System/tree/main/src/main/resources/db/migration

### ⚙️ How it works

* Runs automatically on application startup
* Executes scripts in version order
* Ensures consistent schema across environments

---

## 🔐 Security (JWT)

* JWT-based authentication implemented
* Access Token Expiry: **15 minutes**
* Refresh Token Expiry: **7 days**

---

## 📧 Email Configuration

* Uses Gmail SMTP for sending emails (OTP / notifications)
* TLS and authentication enabled

---

## ▶️ How to Run

1. Clone the repository:

```
git clone https://github.com/Priyanka162001/Core_Mini_Banking_System.git
```

2. Navigate to project:

```
cd Core_Mini_Banking_System
```

3. Configure database & environment variables

4. Run the application:

```
mvn spring-boot:run
```

---

## 🧪 Testing

* Unit and integration tests using **JUnit & Mockito**
* Code coverage using **JaCoCo**
* Includes edge cases and service-level testing

---

## 📖 API Documentation

Swagger UI available at:
http://localhost:8071/swagger-ui.html

---

## 📊 Key Highlights

* Clean layered architecture
* High test coverage
* Real-world banking use cases
* Secure authentication using JWT
* Database versioning using Flyway

---

## 👩‍💻 Author

**Priyanka Meharwade**
