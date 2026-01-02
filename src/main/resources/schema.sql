-- PostgreSQL Database Setup Script for Duebook with Authentication

-- Create database (run this separately as postgres user)
-- CREATE DATABASE duebook_app;

-- Connect to the database
\c duebook_app;

---- Create User ----
-- https://vault.zoho.in#/unlock/extension?routeName=%23%2Fpasscard%2F63500000000007049
CREATE USER duebook_app_user WITH ENCRYPTED PASSWORD '123456';

---- Grant privileges to the user ----
GRANT CONNECT ON DATABASE duebook_app TO duebook_app_user;

---- Create Schema and Set copilot_user as Owner ----
CREATE SCHEMA IF NOT EXISTS duebook_schema AUTHORIZATION duebook_app_user;

---- Grant usage on schema ----
GRANT USAGE, CREATE ON SCHEMA duebook_schema TO duebook_app_user;

-- Set search path to use the schema
SET search_path TO duebook_schema;

-- Create users table with password
CREATE TABLE IF NOT EXISTS duebook_schema.users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    secondary_emails TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_phone ON duebook_schema.users(phone);

-- Create shops table
CREATE TABLE duebook_schema.shops (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_shops_name ON duebook_schema.shops(name);

-- Shop User Roles Enum
CREATE TABLE duebook_schema.shop_users (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES duebook_schema.shops(id),
    user_id BIGINT NOT NULL REFERENCES duebook_schema.users(id),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (shop_id, user_id)
);
CREATE INDEX idx_shop_users_shop ON duebook_schema.shop_users(shop_id);
CREATE INDEX idx_shop_users_user ON duebook_schema.shop_users(user_id);

-- Create customers table
CREATE TABLE duebook_schema.customers (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES duebook_schema.shops(id),
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    opening_balance NUMERIC(12,2) NOT NULL DEFAULT 0,
    current_balance NUMERIC(12,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (shop_id, phone)
);

CREATE INDEX idx_customers_shop ON duebook_schema.customers(shop_id);
CREATE INDEX idx_customers_phone ON duebook_schema.customers(phone);

-- Create customer ledger table
CREATE TABLE duebook_schema.customer_ledger (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES duebook_schema.customers(id),
    shop_id BIGINT NOT NULL REFERENCES duebook_schema.shops(id),
    created_by_user_id BIGINT NOT NULL REFERENCES duebook_schema.users(id),
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    balance_after NUMERIC(12,2) NOT NULL,
    reference_entry_id BIGINT REFERENCES duebook_schema.customer_ledger(id),
    notes TEXT,
    entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_customer ON duebook_schema.customer_ledger(customer_id);
CREATE INDEX idx_ledger_shop ON duebook_schema.customer_ledger(shop_id);
CREATE INDEX idx_ledger_date ON duebook_schema.customer_ledger(entry_date);

CREATE TABLE duebook_schema.payments (
    id BIGSERIAL PRIMARY KEY,
    ledger_entry_id BIGINT NOT NULL REFERENCES duebook_schema.customer_ledger(id),
    mode VARCHAR(20) NOT NULL,
    reference_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE duebook_schema.audit_log (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES duebook_schema.shops(id),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT NOT NULL REFERENCES duebook_schema.users(id),
    old_value JSONB,
    new_value JSONB,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
 );

CREATE INDEX idx_audit_shop ON duebook_schema.audit_log(shop_id);
CREATE INDEX idx_audit_entity ON duebook_schema.audit_log(entity_type, entity_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA duebook_schema TO duebook_app_user;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA duebook_schema TO duebook_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA duebook_schema GRANT SELECT ON SEQUENCES TO duebook_app_user;
