package com.amc.bfsi.dto;

import com.amc.bfsi.entity.Plan;
import com.amc.bfsi.entity.Tenant;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A bank as the API shows it. Fields that do not apply are left out of the JSON. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantResponse {

    private String id;
    private String name;
    private String plan;
    private String status;

    // GET /api/tenant only. A missing limit means "unlimited".
    private Integer maxCustomers;
    private Integer maxUsers;
    private Long customers;
    private Long users;

    public static TenantResponse summary(Tenant t) {
        TenantResponse r = new TenantResponse();
        r.id = t.getId();
        r.name = t.getName();
        r.plan = t.getPlan().name();
        r.status = t.getStatus().name();
        return r;
    }

    public static TenantResponse withUsage(Tenant t, long customers, long users) {
        TenantResponse r = summary(t);
        Plan plan = t.getPlan();
        r.maxCustomers = plan.isUnlimited() ? null : plan.getMaxCustomers();
        r.maxUsers = plan.isUnlimited() ? null : plan.getMaxUsers();
        r.customers = customers;
        r.users = users;
        return r;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPlan() { return plan; }
    public String getStatus() { return status; }
    public Integer getMaxCustomers() { return maxCustomers; }
    public Integer getMaxUsers() { return maxUsers; }
    public Long getCustomers() { return customers; }
    public Long getUsers() { return users; }
}
