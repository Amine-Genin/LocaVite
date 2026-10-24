package ma.locavite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ma.locavite")
public class TestAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestAuthApplication.class, args);
    }
}
