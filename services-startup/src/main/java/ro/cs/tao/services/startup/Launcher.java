package ro.cs.tao.services.startup;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import ro.cs.tao.services.app.ComponentApplication;
import ro.cs.tao.services.app.ConfigurationApplication;
import ro.cs.tao.services.app.TopologyApplication;
import ro.cs.tao.services.monitoring.app.MonitoringApp;
import ro.cs.tao.services.query.Application;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication(scanBasePackages = { "ro.cs.tao.services" })
public class Launcher {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(Application.class, MonitoringApp.class, ComponentApplication.class, ConfigurationApplication.class,
                         TopologyApplication.class)
                .run(args);
    }

}
