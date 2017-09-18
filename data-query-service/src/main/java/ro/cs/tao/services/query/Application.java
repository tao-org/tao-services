package ro.cs.tao.services.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication()
@ImportResource("classpath:data-query-service-context.xml")

public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}