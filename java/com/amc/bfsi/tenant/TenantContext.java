package com.amc.bfsi.tenant;

import java.util.function.Supplier;

/**
 * The tenant (bank) of the request being served, kept per thread.
 * JwtAuthFilter sets it from the token; the login, signup and seeding code use callAs/runAs.
 */
public final class TenantContext {

    /**
     * What Hibernate sees when no tenant is set. It matches no tenant row, so an un-scoped read
     * returns nothing and an un-scoped insert fails the tenant_id foreign key - it can never
     * leak or land in another bank.
     */
    public static final String NONE = "-";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() { }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Never null: {@link #NONE} when nothing is set (this is what Hibernate asks for). */
    public static String current() {
        String tenantId = CURRENT.get();
        return tenantId == null ? NONE : tenantId;
    }

    /** The tenant of the current request; fails loudly when code runs outside one. */
    public static String require() {
        String tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return tenantId;
    }

    /**
     * Runs the task as the given tenant and restores the previous one afterwards.
     * A transaction must START inside the task - Hibernate fixes the tenant when the session opens.
     */
    public static <T> T callAs(String tenantId, Supplier<T> task) {
        String previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return task.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void runAs(String tenantId, Runnable task) {
        callAs(tenantId, () -> {
            task.run();
            return null;
        });
    }
}
