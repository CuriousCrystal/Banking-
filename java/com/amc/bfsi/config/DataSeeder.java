package com.amc.bfsi.config;

import com.amc.bfsi.entity.*;
import com.amc.bfsi.repository.*;
import com.amc.bfsi.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Puts a demo bank, three users and a handful of customers in place so the app is demo-ready.
 * Only runs when app.seed.enabled=true (dev and h2 profiles) - never in production.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    /** The bank code of the demo bank (also the default at login in dev/h2). */
    public static final String DEMO_TENANT = "amc-bank";

    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;

    @Value("${app.seed.enabled:false}")
    private boolean enabled;

    public DataSeeder(TenantRepository tenantRepository, AppUserRepository userRepository,
                      CustomerRepository customerRepository, AccountRepository accountRepository,
                      PasswordEncoder encoder, PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.encoder = encoder;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (!tenantRepository.existsById(DEMO_TENANT)) {
            tenantRepository.save(new Tenant(DEMO_TENANT, "AMC Bank", Plan.ENTERPRISE));
        }
        // the transaction must start INSIDE runAs: Hibernate fixes a session's tenant when the session opens
        TenantContext.runAs(DEMO_TENANT, () -> tx.executeWithoutResult(status -> {
            seedUsers();
            seedCustomers();
        }));
    }

    private void seedUsers() {
        addUser("admin", "admin123", "ADMIN");
        addUser("officer", "officer123", "OFFICER");
        addUser("clerk", "clerk123", "USER");
    }

    private void addUser(String username, String rawPassword, String role) {
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            userRepository.save(new AppUser(username, encoder.encode(rawPassword), role));
        } else if (!isBcryptMatch(rawPassword, user.getPassword()) || !role.equals(user.getRole())) {
            // row left over from an older run (plain-text or different password) - reset the demo login
            user.setPassword(encoder.encode(rawPassword));
            user.setRole(role);
            userRepository.save(user);
        }
    }

    private boolean isBcryptMatch(String raw, String stored) {
        try {
            return stored != null && stored.startsWith("$2") && encoder.matches(raw, stored);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Demo customers (first names only). Each one is added only if its email is not
     * already in the table, so this works even when you have your own data.
     */
    private void seedCustomers() {
        //         name       email                  city         PAN
        customer("Arjun",   "arjun@example.com",   "Bangalore", "ARJPK1001A",
                 acc(AccountType.SAVINGS, "85000.00"), acc(AccountType.CURRENT, "240000.00"),
                 loan("HOME", "1500000.00", "8.40", 240));
        customer("Priya",   "priya@example.com",   "Mumbai",    "PRYPM1002B",
                 acc(AccountType.SAVINGS, "62000.00"),
                 loan("CAR", "450000.00", "9.10", 60));
        customer("Karthik", "karthik@example.com", "Chennai",   "KRTPC1003C",
                 acc(AccountType.SAVINGS, "120000.00"),
                 loan("PERSONAL", "200000.00", "12.50", 36));
        customer("Divya",   "divya@example.com",   "Bangalore", "DVYPB1004D",
                 acc(AccountType.SAVINGS, "45000.00"), acc(AccountType.CURRENT, "90000.00"));
        customer("Suresh",  "suresh@example.com",  "Mumbai",    "SRSPM1005E",
                 acc(AccountType.CURRENT, "310000.00"),
                 loan("HOME", "2500000.00", "8.60", 300), loan("CAR", "600000.00", "9.00", 60));
        customer("Meena",   "meena@example.com",   "Chennai",   "MNAPC1006F",
                 acc(AccountType.SAVINGS, "38000.00"));
        customer("Rahul",   "rahul@example.com",   "Bangalore", "RHLPB1007G",
                 acc(AccountType.SAVINGS, "150000.00"),
                 loan("PERSONAL", "300000.00", "11.90", 48));
        customer("Anjali",  "anjali@example.com",  "Mumbai",    "ANJPM1008H",
                 acc(AccountType.SAVINGS, "72000.00"), acc(AccountType.CURRENT, "55000.00"),
                 loan("CAR", "500000.00", "9.30", 72));
        customer("Vijay",   "vijay@example.com",   "Chennai",   "VJYPC1009J",
                 acc(AccountType.CURRENT, "180000.00"),
                 loan("HOME", "1200000.00", "8.50", 180));
        customer("Neha",    "neha@example.com",    "Bangalore", "NHAPB1010K",
                 acc(AccountType.SAVINGS, "28000.00"));
    }

    private int accountSeq = 9001;   // demo account numbers AC9001, AC9002, ...

    private void customer(String name, String email, String city, String pan, Object... items) {
        if (customerRepository.existsByEmail(email)) {
            return;                                   // already there - leave it alone
        }
        Customer c = new Customer(name, email, city, pan);
        for (Object item : items) {
            if (item instanceof Account a) {
                String number;
                do {
                    number = "AC" + accountSeq++;
                } while (accountRepository.existsByAccountNumber(number));
                a.setAccountNumber(number);           // column is NOT NULL, so set it before saving
                c.addAccount(a);
            } else if (item instanceof Loan l) {
                c.addLoan(l);
            }
        }
        customerRepository.save(c);                   // cascade saves the accounts and loans too
    }

    private static Account acc(AccountType type, String balance) {
        return new Account(type, new java.math.BigDecimal(balance));
    }

    private static Loan loan(String type, String principal, String rate, int months) {
        return new Loan(type, new java.math.BigDecimal(principal), new java.math.BigDecimal(rate), months);
    }
}
