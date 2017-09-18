package ro.cs.tao.services.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication()
@ImportResource("classpath:tao-data-services-context.xml")
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(ComponentApplication.class, TopologyApplication.class, ConfigurationApplication.class)
                .run(args);
    }

}
