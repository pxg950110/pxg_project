package com.maidc.auth.config;

import com.maidc.auth.entity.UserEntity;
import com.maidc.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByUsernameAndOrgIdAndIsDeletedFalse("admin", 0L).ifPresent(user -> {
            // Reset admin password to Admin@123 on every startup
            String encoded = passwordEncoder.encode("Admin@123");
            user.setPasswordHash(encoded);
            user.setStatus("ACTIVE");
            userRepository.save(user);
            log.info("Admin password reset to default");
        });
    }
}
