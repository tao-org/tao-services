package ro.cs.tao.services.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Cosmin Cara
 */
@Configuration
@ImportResource("classpath:monitoring-service-context.xml")
@EnableWebMvc
public class MonitoringServiceLauncer {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringServiceLauncer.class, args);
    }

}
