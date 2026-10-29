-- P6692 MySQL / MariaDB Schema (For Hostinger MySQL Database)
-- Hostinger: hPanel -> Databases -> MySQL Databases -> phpMyAdmin -> Import

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(30) DEFAULT '',
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) DEFAULT '',
    password_salt VARCHAR(64) DEFAULT '',
    pkr_balance DECIMAL(14,2) DEFAULT 0.00,
    demo_balance DECIMAL(14,2) DEFAULT 5000.00,
    vip_level INT DEFAULT 1,
    role VARCHAR(20) DEFAULT 'PLAYER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS deposits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) UNIQUE NOT NULL,
    method VARCHAR(30) DEFAULT 'JAZZCASH',
    amount_pkr DECIMAL(14,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'PKR',
    sender_mobile VARCHAR(30),
    jazzcash_txn_ref VARCHAR(100),
    status VARCHAR(30) DEFAULT 'PENDING',
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS withdrawals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    withdrawal_id VARCHAR(64) UNIQUE NOT NULL,
    method VARCHAR(30) DEFAULT 'JAZZCASH',
    amount_pkr DECIMAL(14,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'PKR',
    account_title VARCHAR(150),
    account_number VARCHAR(50),
    jazzcash_txn_ref VARCHAR(100),
    status VARCHAR(30) DEFAULT 'PENDING',
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    transaction_id VARCHAR(64) UNIQUE NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount_pkr DECIMAL(14,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'PKR',
    previous_balance DECIMAL(14,2) NOT NULL,
    new_balance DECIMAL(14,2) NOT NULL,
    status VARCHAR(30) DEFAULT 'SUCCESSFUL',
    payment_provider VARCHAR(30) DEFAULT 'JAZZCASH',
    provider_txn_id VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Initial seed data
INSERT IGNORE INTO users (id, username, mobile_number, email, pkr_balance, demo_balance, vip_level, role) VALUES 
('p6692_user_1', 'CyberKnight_92', '03001234567', 'player@p6692.com', 0.00, 5000.00, 1, 'PLAYER'),
('p6692_admin_1', 'P6692_Admin', '03009999999', 'admin@p6692.com', 0.00, 5000.00, 10, 'ADMIN');
