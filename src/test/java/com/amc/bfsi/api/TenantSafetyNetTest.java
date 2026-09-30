package com.amc.bfsi.api;

import com.amc.bfsi.entity.Customer;
import com.amc.bfsi.entity.Plan;
import com.amc.bfsi.entity.Tenant;
import com.amc.bfsi.repository.CustomerRepository;
import com.amc.bfsi.repository.TenantRepository;
import com.amc.bfsi.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hibernate's tenant filter covers queries but not em.find(). The repositories route findById through
 * a query, and TenantScoped fails closed for anything that slips past - this proves both layers.
 */
@SpringBootTest
@ActiveProfiles("h2")
class TenantSafetyNetTest {

    @Autowired TenantRepository tenants;
    @Autowired CustomerRepository customers;
    @Autowired PlatformTransactionManager transactionManager;
    @PersistenceContext EntityManager em;

    @Test
    void aLookupThatBypassesTheTenantFilterFailsInsteadOfLeaking() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String owner = ApiClient.uniqueCode("safe-a");
        String intruder = ApiClient.uniqueCode("safe-b");
        tenants.save(new Tenant(owner, "Owner Bank", Plan.PRO));
        tenants.save(new Tenant(intruder, "Intruder Bank", Plan.PRO));

        // the transaction must start inside callAs: Hibernate fixes a session's tenant when it opens
        Long id = TenantContext.callAs(owner, () -> tx.execute(status ->
                customers.save(new Customer("Owned Customer", "owned@example.com", "Pune", "ABCDE1234F"))
                        .getCustomerId()));

        // layer 2: the raw em.find() route is the one the tenant filter misses - it must throw, not return the row
        assertThatThrownBy(() -> TenantContext.runAs(intruder, () -> tx.executeWithoutResult(status ->
                em.find(Customer.class, id))))
                .hasStackTraceContaining("Refusing to load a Customer of another bank");

        // layer 1: the normal repository route simply says "not found" for the same id ...
        assertThat(TenantContext.callAs(intruder, () -> customers.findById(id))).isEmpty();
        // ... while the owner still finds it
        assertThat(TenantContext.callAs(owner, () -> customers.findById(id))).isPresent();
    }
}
