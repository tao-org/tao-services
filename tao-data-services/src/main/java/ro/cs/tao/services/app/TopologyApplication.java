package ro.cs.tao.services.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication(scanBasePackages = { "ro.cs.tao.services" })
public class TopologyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TopologyApplication.class, args);
    }

}
