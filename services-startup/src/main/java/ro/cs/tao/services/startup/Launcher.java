package ro.cs.tao.services.startup;

import org.springframework.boot.ApplicationHome;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.app.ComponentApplication;
import ro.cs.tao.services.app.ConfigurationApplication;
import ro.cs.tao.services.app.ContainerApplication;
import ro.cs.tao.services.app.TopologyApplication;
import ro.cs.tao.services.monitoring.app.MonitoringApp;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication()
@EnableScheduling
@ImportResource({"classpath:services-startup-context.xml", "classpath*:tao-persistence-context.xml" })
public class Launcher {
    private static final ApplicationHome home;

    static {
        home = new ApplicationHome(Launcher.class);
        try {
            Path configDirectory = homeDirectory().resolve("config");
            if (!Files.exists(configDirectory)) {
                Files.createDirectory(configDirectory);
            }
            final Field field = ConfigurationManager.class.getDeclaredField("configFolder");
            field.setAccessible(true);
            field.set(null, configDirectory);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //LifeCycleProcessor.activate();
        new SpringApplicationBuilder()
                .sources(MonitoringApp.class, ComponentApplication.class, ConfigurationApplication.class,
                         TopologyApplication.class, ContainerApplication.class)
                .run(args);
    }

    private static Path homeDirectory() { return home.getDir().getParentFile().toPath(); }
}
