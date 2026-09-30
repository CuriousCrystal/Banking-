package com.amc.bfsi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

/**
 * One bank on the platform. Not tenant-scoped itself: it IS the tenant.
 * The id is the bank code the user types at login (e.g. "amc-bank") and never changes.
 */
@Entity
@Table(name = "tenant")
public class Tenant implements Persistable<String> {

    public enum Status { ACTIVE, SUSPENDED }

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    // plain VARCHAR columns, not MySQL's native ENUM (see the note on Account.accountType)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Plan plan = Plan.FREE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // The id is assigned by us, so Spring Data cannot tell "new" from "existing" by a null id.
    // Without this, save() would MERGE - and silently overwrite an existing bank that has the same code.
    @Transient
    private boolean isNew = true;

    protected Tenant() { }

    public Tenant(String id, String name, Plan plan) {
        this.id = id;
        this.name = name;
        this.plan = plan;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getId() { return id; }
    @Override
    public boolean isNew() { return isNew; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
