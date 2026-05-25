package com.site.meetingandclass;

import com.site.meetingandclass.enums.AccountStatus;
import com.site.meetingandclass.enums.Role;
import com.site.meetingandclass.model.User;
import com.site.meetingandclass.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the SUPER_ADMIN account on every startup IF it does not already exist.
 * On a new PC / fresh database this runs automatically and creates the account.
 * If the account already exists (same PC, restart) it does nothing.
 *
 * Credentials:
 *   Email   : admin@cuniv-naama.dz
 *   Password: Admin@2025!
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String SUPER_ADMIN_EMAIL    = "admin@cuniv-naama.dz";
    private static final String SUPER_ADMIN_PASSWORD = "Admin@2025!";

    @Override
    public void run(String... args) {
        // Only seed if the account does not exist yet (fresh DB on any machine)
        if (userRepository.findByEmail(SUPER_ADMIN_EMAIL).isPresent()) {
            System.out.println("✅ SUPER_ADMIN already exists — skipping seed.");
            return;
        }

        User superAdmin = new User();
        superAdmin.setFullName("Super Administrator");
        superAdmin.setEmail(SUPER_ADMIN_EMAIL);
        superAdmin.setPassword(passwordEncoder.encode(SUPER_ADMIN_PASSWORD));
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setStatus(AccountStatus.APPROVED);
        superAdmin.setEnabled(true);
        userRepository.save(superAdmin);

        System.out.println("✅ SUPER_ADMIN seeded successfully!");
        System.out.println("   Email   : " + SUPER_ADMIN_EMAIL);
        System.out.println("   Password: " + SUPER_ADMIN_PASSWORD);
    }
}
