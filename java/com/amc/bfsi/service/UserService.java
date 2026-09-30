package com.amc.bfsi.service;

import com.amc.bfsi.dto.UserRequest;
import com.amc.bfsi.entity.AppUser;
import com.amc.bfsi.exception.BusinessRuleException;
import com.amc.bfsi.exception.DuplicateResourceException;
import com.amc.bfsi.exception.ResourceNotFoundException;
import com.amc.bfsi.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder encoder;      // the BCryptPasswordEncoder bean from SecurityConfig

    public UserService(AppUserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public List<AppUser> findAll() {
        return repository.findAll();
    }

    @Transactional
    public AppUser create(UserRequest request) {
        String username = request.getUsername().trim();
        if (repository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username '" + username + "' already exists");
        }
        // THE key line: plain text in, BCrypt hash out ("$2a$10$..."). Only the hash is stored.
        String hash = encoder.encode(request.getPassword());
        return repository.save(new AppUser(username, hash, request.getRole()));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 72) {
            throw new BusinessRuleException("Password must be between 6 and 72 characters");
        }
        AppUser user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id));
        user.setPassword(encoder.encode(newPassword));
        repository.save(user);
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        AppUser user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id));
        if (user.getUsername().equals(currentUsername)) {
            throw new BusinessRuleException("You cannot delete the account you are logged in with");
        }
        repository.delete(user);
    }
}
