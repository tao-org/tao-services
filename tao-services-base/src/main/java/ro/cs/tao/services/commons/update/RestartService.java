package ro.cs.tao.services.commons.update;

import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import ro.cs.tao.docker.Application;

import java.util.logging.Logger;

/**
 * Service responsible for restarting the application context within a Spring Boot application.
 *
 * This class implements the {@link ApplicationContextAware} interface,
 * allowing it to access and manage the {@link ApplicationContext}.
 * The main functionality provided by this service is to trigger the restart of the application
 * by stopping the current application context and restarting it in a separate thread.
 *
 * An instance of this service can be managed by Spring's dependency injection framework and is
 * annotated with {@link Service} for registration as a Spring bean.
 *
 * Threading Note:
 * The restart operation is performed in a new thread to ensure the shutdown and subsequent
 * reinitialization of the application context do not block the initiating request.
 *
 * Methods:
 * - {@link #setApplicationContext(ApplicationContext)}: Sets the application context instance for this service.
 * - {@link #doRestart()}: Initiates the restart process of the application context.
 *
 * Usage:
 * This service can be triggered by other components (e.g., REST controllers or schedulers)
 * to programmatically restart the application and apply potential configuration changes or updates.
 */
@Service("restartService")
public class RestartService implements ApplicationContextAware {
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public void doRestart() {
        Logger.getLogger(RestartService.class.getName()).info("Services restart initiated");
        final ApplicationArguments args = context.getBean(ApplicationArguments.class);
        final Thread thread = new Thread(() -> {
            Logger.getLogger(RestartService.class.getName()).info("Services are restarting");
            ((ConfigurableApplicationContext) context).close();
            context = SpringApplication.run(Application.class, args.getSourceArgs());
        });
        thread.setDaemon(false);
        thread.start();
    }
}
