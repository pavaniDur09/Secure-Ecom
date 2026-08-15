package com.ecommerce.config;

import com.ecommerce.model.entity.Tenant;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.UserRole;
import com.ecommerce.repository.TenantRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once automatically when the app starts. Creates a default tenant and an
 * admin login so you can test the API immediately without registering first.
 *
 * Default admin login:
 *   email:    admin@tenant1.com
 *   password: Admin123!
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (tenantRepository.findByName("tenant1").isEmpty()) {
            Tenant tenant = new Tenant();
            tenant.setName("tenant1");
            tenant.setDescription("Default demo shop");
            tenantRepository.save(tenant);
        }

        if (userRepository.findByEmail("admin@tenant1.com").isEmpty()) {
            User admin = User.builder()
                    .email("admin@tenant1.com")
                    .password(passwordEncoder.encode("Admin123!"))
                    .role(UserRole.ADMIN)
                    .tenantId("tenant1")
                    .accountNonLocked(true)
                    .failedLoginAttempts(0)
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Seeded default admin: admin@tenant1.com / Admin123!");
        }
    }
}
