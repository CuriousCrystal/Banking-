-- Multi-tenant baseline schema.
-- Written in plain SQL that MySQL 8 and H2 (MySQL mode) both accept, so the test suite runs the very same migration.
-- Every business table carries tenant_id: Hibernate (@TenantId) filters reads and stamps writes with it,
-- the foreign key makes an un-scoped insert fail instead of landing in the wrong bank.

CREATE TABLE tenant (
    id          VARCHAR(40)  NOT NULL,          -- the bank code (slug); immutable
    name        VARCHAR(100) NOT NULL,
    plan        VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE app_user (
    user_id    BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id  VARCHAR(40)  NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,           -- BCrypt hash
    role       VARCHAR(20)  NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_app_user_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT fk_app_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE customer (
    customer_id  BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(40)  NOT NULL,
    name         VARCHAR(60)  NOT NULL,
    email        VARCHAR(120) NOT NULL,
    city         VARCHAR(50),
    pan_number   VARCHAR(10),
    created_on   DATE,
    PRIMARY KEY (customer_id),
    CONSTRAINT uk_customer_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_customer_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);
CREATE INDEX idx_customer_tenant_city ON customer (tenant_id, city);

CREATE TABLE account (
    account_id      BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(40)   NOT NULL,
    customer_id     BIGINT        NOT NULL,
    account_number  VARCHAR(20)   NOT NULL,
    account_type    VARCHAR(20)   NOT NULL,
    account_status  VARCHAR(10)   NOT NULL,
    balance         DECIMAL(14,2) NOT NULL,
    opened_on       DATE,
    row_version     BIGINT,                      -- optimistic-lock counter (@Version)
    PRIMARY KEY (account_id),
    CONSTRAINT uk_account_tenant_number UNIQUE (tenant_id, account_number),
    CONSTRAINT fk_account_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenant (id),
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
);
CREATE INDEX idx_account_customer ON account (customer_id);

CREATE TABLE loan (
    loan_id           BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id         VARCHAR(40)   NOT NULL,
    customer_id       BIGINT        NOT NULL,
    loan_type         VARCHAR(20)   NOT NULL,
    principal_amount  DECIMAL(14,2) NOT NULL,
    interest_rate     DECIMAL(5,2)  NOT NULL,
    tenure_months     INT           NOT NULL,
    sanctioned_on     DATE,
    PRIMARY KEY (loan_id),
    CONSTRAINT fk_loan_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenant (id),
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
);
CREATE INDEX idx_loan_customer ON loan (customer_id);
