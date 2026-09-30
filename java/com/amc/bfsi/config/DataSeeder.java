package com.amc.bfsi.config;

import com.amc.bfsi.entity.*;
import com.amc.bfsi.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Puts three users and a handful of customers in place so the app is demo-ready. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder encoder;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    public DataSeeder(AppUserRepository userRepository, CustomerRepository customerRepository,
                      AccountRepository accountRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        seedUsers();
        seedCustomers();
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
                 acc(AccountType.SAVINGS, 85000.0), acc(AccountType.CURRENT, 240000.0),
                 loan("HOME", 1500000.0, 8.4, 240));
        customer("Priya",   "priya@example.com",   "Mumbai",    "PRYPM1002B",
                 acc(AccountType.SAVINGS, 62000.0),
                 loan("CAR", 450000.0, 9.1, 60));
        customer("Karthik", "karthik@example.com", "Chennai",   "KRTPC1003C",
                 acc(AccountType.SAVINGS, 120000.0),
                 loan("PERSONAL", 200000.0, 12.5, 36));
        customer("Divya",   "divya@example.com",   "Bangalore", "DVYPB1004D",
                 acc(AccountType.SAVINGS, 45000.0), acc(AccountType.CURRENT, 90000.0));
        customer("Suresh",  "suresh@example.com",  "Mumbai",    "SRSPM1005E",
                 acc(AccountType.CURRENT, 310000.0),
                 loan("HOME", 2500000.0, 8.6, 300), loan("CAR", 600000.0, 9.0, 60));
        customer("Meena",   "meena@example.com",   "Chennai",   "MNAPC1006F",
                 acc(AccountType.SAVINGS, 38000.0));
        customer("Rahul",   "rahul@example.com",   "Bangalore", "RHLPB1007G",
                 acc(AccountType.SAVINGS, 150000.0),
                 loan("PERSONAL", 300000.0, 11.9, 48));
        customer("Anjali",  "anjali@example.com",  "Mumbai",    "ANJPM1008H",
                 acc(AccountType.SAVINGS, 72000.0), acc(AccountType.CURRENT, 55000.0),
                 loan("CAR", 500000.0, 9.3, 72));
        customer("Vijay",   "vijay@example.com",   "Chennai",   "VJYPC1009J",
                 acc(AccountType.CURRENT, 180000.0),
                 loan("HOME", 1200000.0, 8.5, 180));
        customer("Neha",    "neha@example.com",    "Bangalore", "NHAPB1010K",
                 acc(AccountType.SAVINGS, 28000.0));
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

    private static Account acc(AccountType type, double balance) {
        return new Account(type, balance);
    }

    private static Loan loan(String type, double principal, double rate, int months) {
        return new Loan(type, principal, rate, months);
    }
}
