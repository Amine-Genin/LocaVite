package ma.locavite.auth.config;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
public class DevAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminInitializer.class);

    public static final String DEV_ADMIN_EMAIL = "admin.dev@locavite.ma";
    public static final String DEV_ADMIN_PASSWORD = "AdminDev2026!";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DevAdminInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(DEV_ADMIN_EMAIL)) {
            Role adminRole = roleRepository.findByName(RoleType.ADMIN.name())
                    .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ADMIN.name()).build()));

            User admin = User.builder()
                    .nom("Admin")
                    .prenom("Dev")
                    .email(DEV_ADMIN_EMAIL)
                    .telephone("+212600000000")
                    .password(passwordEncoder.encode(DEV_ADMIN_PASSWORD))
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);
            log.info("Compte administrateur de développement initialisé avec succès : {}", DEV_ADMIN_EMAIL);
        }
    }
}
