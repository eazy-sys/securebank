-- Drop existing tables
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS flyway_schema_history;

-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create accounts table
CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    pin VARCHAR(255) NOT NULL,
    balance DECIMAL(19,2) DEFAULT 0.00,
    account_type VARCHAR(20) NOT NULL DEFAULT 'SAVINGS',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create transactions table
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(19,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source_account_id BIGINT,
    target_account_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    FOREIGN KEY (target_account_id) REFERENCES accounts(id)
); 