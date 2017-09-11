package ro.cs.tao.services.impl;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import ro.cs.tao.services.app.ComponentApplication;
import ro.cs.tao.services.app.TopologyApplication;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication(scanBasePackages = { "ro.cs.tao.services" })
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(ComponentApplication.class, TopologyApplication.class)
                .run(args);
    }

}
