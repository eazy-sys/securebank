-- Drop existing tables if they exist
DROP TABLE IF EXISTS token;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;

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
    pin VARCHAR(255),
    balance DECIMAL(19,2) DEFAULT 0.00,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create transaction table
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(19,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source_account_id BIGINT,
    target_account_id BIGINT,
    FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    FOREIGN KEY (target_account_id) REFERENCES accounts(id)
);

-- Create token table
CREATE TABLE token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
); 