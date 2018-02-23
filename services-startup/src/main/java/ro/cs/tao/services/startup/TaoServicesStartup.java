package ro.cs.tao.services.startup;

import org.springframework.boot.ApplicationHome;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.entity.DataServicesLauncher;
import ro.cs.tao.services.monitoring.MonitoringServiceLauncer;
import ro.cs.tao.services.progress.ProgressReportLauncher;
import ro.cs.tao.services.query.DataQueryServiceLauncher;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication()
@EnableScheduling
@ImportResource("classpath*:tao-persistence-context.xml")
public class TaoServicesStartup {
    private static final ApplicationHome home;

    static {
        home = new ApplicationHome(TaoServicesStartup.class);
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
                .sources(MonitoringServiceLauncer.class, DataServicesLauncher.class,
                         DataQueryServiceLauncher.class, ProgressReportLauncher.class)
                .run(args);
    }

    private static Path homeDirectory() { return home.getDir().getParentFile().toPath(); }
}
