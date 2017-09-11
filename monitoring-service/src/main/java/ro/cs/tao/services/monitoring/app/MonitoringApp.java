package ro.cs.tao.services.monitoring.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication(scanBasePackages = { "ro.cs.tao.services.monitoring" })
public class MonitoringApp {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringApp.class, args);
    }

}
