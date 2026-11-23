package com.telehealth.auth.config;

import com.telehealth.auth.model.Role;
import com.telehealth.auth.model.RoleName;
import com.telehealth.auth.model.User;
import com.telehealth.auth.repository.RoleRepository;
import com.telehealth.auth.repository.UserRepository;
import kpi.kushnir.privacy.neo4j.UserIdentityNode;
import kpi.kushnir.privacy.neo4j.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserIdentityRepository userIdentityRepository;

    @Override
    public void run(String... args) {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        });

        // TODO: take data from files not hardcoded
        String adminEmail = "admin@telehealth.ua";
        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("Admin12345!"))
                    .isActive(true)
                    .is2faEnabled(true)
                    .roles(Set.of(adminRole))
                    .build();

            User savedAdmin = userRepository.save(admin);
            userIdentityRepository.save(new UserIdentityNode(savedAdmin.getId().toString()));
        }
    }
}
