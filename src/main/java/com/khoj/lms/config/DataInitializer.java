package com.khoj.lms.config;

import com.khoj.lms.entity.Role;
import com.khoj.lms.entity.User;
import com.khoj.lms.enums.RoleName;
import com.khoj.lms.repository.RoleRepository;
import com.khoj.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Initialize roles
        for (RoleName roleName : RoleName.values()) {

            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(roleName);
                        return roleRepository.save(role);
                    });
        }

        System.out.println("✅ Roles initialized");

        // Create admin user if not exists
        if (userRepository
                .findByEmailAndIsDeletedFalse("admin@khoj.com")
                .isEmpty()) {

            User admin = new User();

            admin.setFirstName("Super");
            admin.setLastName("Admin");

            admin.setEmail("admin@khoj.com");
            admin.setPassword(passwordEncoder.encode("Admin123"));

            admin.setIsEmailVerified(true);
            admin.setIsActive(true);

            Role adminRole = roleRepository
                    .findByName(RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            admin.setRoles(Set.of(adminRole));

            userRepository.save(admin);

            System.out.println("✅ Admin user created");
        }
    }
}