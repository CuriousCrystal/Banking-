package com.amc.bfsi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "account",
       uniqueConstraints = @UniqueConstraint(name = "uk_account_tenant_number", columnNames = {"tenant_id", "account_number"}))
public class Account extends TenantScoped {

    public enum AccountStatus { ACTIVE, FROZEN, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    // unique per bank, not globally (see uk_account_tenant_number)
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    // Enums are plain VARCHAR columns (see V1 migration). Without @JdbcTypeCode Hibernate's MySQL dialect
    // expects a native ENUM column and schema validation fails there - H2 would not have told us.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /** Money is BigDecimal - double accumulates rounding errors. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "account_status", nullable = false, length = 10)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "opened_on")
    private LocalDate openedOn = LocalDate.now();

    /**
     * Optimistic-lock counter: JPA increments it on every update and rejects
     * a save whose version no longer matches the database (409 to the client).
     */
    @Version
    @Column(name = "row_version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Account() { }

    public Account(AccountType accountType, BigDecimal balance) {
        this.accountType = accountType;
        this.balance = balance;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDate getOpenedOn() { return openedOn; }
    public void setOpenedOn(LocalDate openedOn) { this.openedOn = openedOn; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
