package ro.cs.tao.services.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication()
@ImportResource("classpath:tao-data-services-context.xml")
public class TopologyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TopologyApplication.class, args);
    }

}
