package com.linkgrove.api.config;

import com.linkgrove.api.model.Role;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.RoleRepository;
import com.linkgrove.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role userRole = roleRepository.findByName("USER").orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        // Ensure admin user exists and has ADMIN + USER roles
        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User created = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(new HashSet<>())
                    .build();
            created.getRoles().add(adminRole);
            created.getRoles().add(userRole);
            log.info("Created default admin user");
            return userRepository.save(created);
        });
        boolean adminChanged = false;
        if (admin.getRoles().stream().noneMatch(r -> "ADMIN".equals(r.getName()))) {
            admin.getRoles().add(adminRole);
            adminChanged = true;
        }
        if (admin.getRoles().stream().noneMatch(r -> "USER".equals(r.getName()))) {
            admin.getRoles().add(userRole);
            adminChanged = true;
        }
        if (adminChanged) {
            userRepository.save(admin);
            log.info("Ensured admin has ADMIN and USER roles");
        }

        // Ensure demo user exists and has USER role
        User demo = userRepository.findByUsername("demo").orElseGet(() -> {
            User created = User.builder()
                    .username("demo")
                    .email("demo@example.com")
                    .password(passwordEncoder.encode("password"))
                    .roles(new HashSet<>())
                    .build();
            created.getRoles().add(userRole);
            log.info("Created default demo user");
            return userRepository.save(created);
        });
        if (demo.getRoles().stream().noneMatch(r -> "USER".equals(r.getName()))) {
            demo.getRoles().add(userRole);
            userRepository.save(demo);
            log.info("Ensured demo has USER role");
        }
    }
}
