package com.amc.bfsi.entity;

import com.amc.bfsi.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import org.hibernate.annotations.TenantId;

/**
 * Base class of everything that belongs to one bank. Hibernate filters every query by tenant_id and
 * stamps it on insert, so services never pass the bank around and cannot forget it.
 */
@MappedSuperclass
public abstract class TenantScoped {

    /** Owning bank. Set by Hibernate from the current tenant - never by hand. */
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 40)
    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }

    /**
     * Fail-closed safety net. The tenant filter covers queries; if some code path ever loads a row by a
     * route that bypasses it, this turns a silent cross-bank leak into an exception.
     */
    @PostLoad
    void requireSameTenant() {
        if (!TenantContext.current().equals(tenantId)) {
            throw new IllegalStateException("Refusing to load a " + getClass().getSimpleName() + " of another bank");
        }
    }
}
