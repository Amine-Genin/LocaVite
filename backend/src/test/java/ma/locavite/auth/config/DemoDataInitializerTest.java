package ma.locavite.auth.config;

import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DemoDataInitializerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSeedDemoDataCleanlyAndIdempotently() {
        DemoDataInitializer initializer = new DemoDataInitializer(
                userRepository, roleRepository, passwordEncoder, jdbcTemplate
        );

        initializer.run();

        Integer reservationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE code LIKE 'RES-DEMO-%'",
                Integer.class
        );
        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE code LIKE 'PAY-DEMO-%'",
                Integer.class
        );
        Integer vehicleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicles",
                Integer.class
        );

        assertThat(reservationCount).isEqualTo(8);
        assertThat(paymentCount).isEqualTo(8);
        assertThat(vehicleCount).isGreaterThanOrEqualTo(8);

        // Second execution must be idempotent
        initializer.run();
        Integer reservationCountAfterSecondRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE code LIKE 'RES-DEMO-%'",
                Integer.class
        );
        assertThat(reservationCountAfterSecondRun).isEqualTo(8);
    }
}
