package ie.universityofgalway.finance.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Reporting service.
 */
@SpringBootApplication
public class ReportingServiceApplication {
    /**
     * Starts the application context and embedded web server.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
