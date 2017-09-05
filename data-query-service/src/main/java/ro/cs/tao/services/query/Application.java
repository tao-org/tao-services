package ro.cs.tao.services.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication(scanBasePackages = { "ro.cs.tao.services.query" })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}