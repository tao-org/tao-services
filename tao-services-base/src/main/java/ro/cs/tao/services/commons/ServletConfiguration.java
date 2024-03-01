package ro.cs.tao.services.commons;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ro.cs.tao.configuration.ConfigurationManager;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public abstract class ServletConfiguration {

    private static final int MAGIC = 0xcafebabe;
    private static final Map<String, String> javaVersions = new HashMap<String, String>() {{
        put("45.0", "1.0"); put("45.3", "1.1"); put("46.0", "1.2"); put("47.0", "1.3"); put("48.0", "1.4");
        put("49.0", "5"); put("50.0", "6"); put("51.0", "7"); put("52.0", "8"); put("53.0", "9");
        put("54.0", "10"); put("55.0", "11"); put("56.0", "12"); put("57.0", "13"); put("58.0", "14");
        put("59.0", "15"); put("60.0", "16"); put("61.0", "17"); put("62.0", "18"); put("63.0", "19");
        put("64.0", "20"); put("65.0", "21");
    }};

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return (container -> {
            String strPort = ConfigurationManager.getInstance().getValue("server.port");
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
        if (version.isEmpty()) {
            version.put("TAO Services", "running from IDE");
        }
        return version;
    }

    protected Map<String, String> readManifest() {
        Map<String, String> entries = new HashMap<>();
        try {
            final Class<? extends ServletConfiguration> clazz = getClass();
            Enumeration<URL> resources = clazz.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try (InputStream inputStream = resources.nextElement().openStream()) {
                    Manifest manifest = new Manifest(inputStream);
                    Attributes attributes = manifest.getMainAttributes();
                    String value = attributes.getValue("Bundle-Name");
                    if (value != null && value.startsWith("TAO")) {
                        final String version = attributes.getValue("Version");
                        ConfigurationManager.getInstance().setValue("tao.version", version);
                        entries.put("TAO Services", version + " (" + attributes.getValue("Build-Time") + ")");
                    }
                }
            }
            JarFile jarFile = new JarFile(Paths.get(ServletConfiguration.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile());
            try (DataInputStream in  = new DataInputStream(jarFile.getInputStream(jarFile.getJarEntry(ServletConfiguration.class.getName().replace(".", "/") + ".class")))) {
                int magic = in.readInt();
                if (magic != MAGIC) {
                    System.out.println("Magic number not found");
                }
                int minor = in.readUnsignedShort();
                int major = in.readUnsignedShort();
                final String version = javaVersions.get(major + "." + minor);
                entries.put("Compiler", version != null ? version : "unknown");
            }
        } catch (Exception ignored) {
        }
        return entries;
    }

}
