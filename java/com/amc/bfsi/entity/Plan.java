package com.amc.bfsi.entity;

/** Subscription plan of a tenant (bank). The limits are enforced by PlanGuard. */
public enum Plan {

    FREE(25, 3),
    PRO(1000, 25),
    ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int maxCustomers;
    private final int maxUsers;

    Plan(int maxCustomers, int maxUsers) {
        this.maxCustomers = maxCustomers;
        this.maxUsers = maxUsers;
    }

    public int getMaxCustomers() { return maxCustomers; }
    public int getMaxUsers() { return maxUsers; }
    public boolean isUnlimited() { return maxCustomers == Integer.MAX_VALUE; }
}
