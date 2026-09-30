package com.amc.bfsi.service;

import com.amc.bfsi.dto.SignupRequest;
import com.amc.bfsi.dto.TenantResponse;
import com.amc.bfsi.entity.Plan;
import com.amc.bfsi.entity.Tenant;
import com.amc.bfsi.exception.DuplicateResourceException;
import com.amc.bfsi.repository.AppUserRepository;
import com.amc.bfsi.repository.CustomerRepository;
import com.amc.bfsi.repository.TenantRepository;
import com.amc.bfsi.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.Set;

@Service
public class TenantService {

    /** Bank codes that would clash with routes or look official. */
    private static final Set<String> RESERVED =
            Set.of("admin", "api", "app", "default", "login", "platform", "signup", "static", "system", "www");

    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository userRepository;
    private final UserService userService;
    private final TransactionTemplate tx;

    public TenantService(TenantRepository tenantRepository, CustomerRepository customerRepository,
                         AppUserRepository userRepository, UserService userService,
                         PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** The bank, only if it exists and is not suspended. */
    public Optional<Tenant> findActive(String tenantId) {
        return tenantRepository.findById(tenantId).filter(t -> t.getStatus() == Tenant.Status.ACTIVE);
    }

    /** The current bank with its plan limits and how much of them is used. */
    public TenantResponse current() {
        Tenant tenant = tenantRepository.findById(TenantContext.require())
                .orElseThrow(() -> new IllegalStateException("Unknown tenant " + TenantContext.current()));
        return TenantResponse.withUsage(tenant, customerRepository.count(), userRepository.count());
    }

    /** Creates the bank (FREE plan) and its first ADMIN user. Not @Transactional on purpose - see below. */
    public Tenant signup(SignupRequest request) {
        String id = request.getBankCode();
        if (RESERVED.contains(id) || tenantRepository.existsById(id)) {
            throw new DuplicateResourceException("Bank code '" + id + "' is not available");
        }

        Tenant tenant;
        try {
            tenant = tenantRepository.save(new Tenant(id, request.getBankName().trim(), Plan.FREE));
        } catch (DataIntegrityViolationException raced) {
            throw new DuplicateResourceException("Bank code '" + id + "' is not available");
        }

        try {
            // Hibernate fixes a session's tenant when the session opens, so the user must be created in a
            // NEW transaction that starts after the tenant context is set (a method-level @Transactional
            // here would open the session too early, for "no tenant").
            TenantContext.runAs(id, () -> tx.executeWithoutResult(status ->
                    userService.createUser(request.getUsername().trim(), request.getPassword(), "ADMIN")));
        } catch (RuntimeException e) {
            tenantRepository.deleteById(id);            // never leave a bank without an admin behind
            throw e;
        }
        return tenant;
    }
}
