package com.amc.bfsi.service;

import com.amc.bfsi.entity.Plan;
import com.amc.bfsi.exception.PlanLimitException;
import com.amc.bfsi.repository.AppUserRepository;
import com.amc.bfsi.repository.CustomerRepository;
import com.amc.bfsi.repository.TenantRepository;
import com.amc.bfsi.tenant.TenantContext;
import org.springframework.stereotype.Service;

/**
 * Enforces what the current bank's subscription plan allows. The counts are tenant-scoped
 * automatically, so "how many customers do we have" is always this bank's number.
 *
 * ponytail: check-then-insert, so two simultaneous requests can overshoot a limit by one.
 * Fine for a soft plan limit; lock the tenant row if it ever has to be exact.
 */
@Service
public class PlanGuard {

    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository userRepository;

    public PlanGuard(TenantRepository tenantRepository, CustomerRepository customerRepository,
                     AppUserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    public void checkCustomerCapacity() {
        Plan plan = currentPlan();
        if (customerRepository.count() >= plan.getMaxCustomers()) {
            throw new PlanLimitException("The " + plan + " plan allows up to " + plan.getMaxCustomers()
                    + " customers - upgrade your plan to add more");
        }
    }

    public void checkUserCapacity() {
        Plan plan = currentPlan();
        if (userRepository.count() >= plan.getMaxUsers()) {
            throw new PlanLimitException("The " + plan + " plan allows up to " + plan.getMaxUsers()
                    + " users - upgrade your plan to add more");
        }
    }

    private Plan currentPlan() {
        return tenantRepository.findById(TenantContext.require())
                .orElseThrow(() -> new IllegalStateException("Unknown tenant " + TenantContext.current()))
                .getPlan();
    }
}
