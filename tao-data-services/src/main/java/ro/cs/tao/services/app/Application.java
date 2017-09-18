package ro.cs.tao.services.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication(scanBasePackages = { "ro.cs.tao.services", "ro.cs.tao.persistence" })
@ImportResource("classpath:tao-persistence-context.xml")
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(ComponentApplication.class, TopologyApplication.class, ConfigurationApplication.class)
                .run(args);
    }

}
