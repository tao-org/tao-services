package ro.cs.tao.services.commons;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ro.cs.tao.configuration.Configuration;
import ro.cs.tao.configuration.ConfigurationManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public abstract class ServletConfiguration {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return (container -> {
            String strPort = ConfigurationManager.getInstance().getValue(Configuration.Services.PORT);
            int port = strPort != null ? Integer.parseInt(strPort) : 8080;
            final Logger logger = Logger.getLogger(ServletConfiguration.class.getName());
            final Map<String, String> map = versionInfo();
            if (map != null) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    logger.info(String.format("%s version: %s", entry.getKey(), entry.getValue()));
                }
            }
            logger.info("Using server port " + port);
            container.setPort(port);
        });
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(10);
        taskExecutor.initialize();
        return taskExecutor;
    }

    protected Map<String, String> versionInfo() {
        final Map<String, String> version = readManifest();
        if (version.size() == 0) {
            version.put("TAO Services", "running from IDE");
        }
        return version;
    }

    protected Map<String, String> readManifest() {
        Map<String, String> entries = new HashMap<>();
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try (InputStream inputStream = resources.nextElement().openStream()) {
                    Manifest manifest = new Manifest(inputStream);
                    Attributes attributes = manifest.getMainAttributes();
                    String value = attributes.getValue("Bundle-Name");
                    if (value != null && value.startsWith("TAO")) {
                        entries.put("TAO Services", attributes.getValue("Version") + " (" + attributes.getValue("Build-Time") + ")");
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return entries;
    }

}
