package ro.cs.tao.services.query;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Cosmin Cara
 */
@Configuration
@ImportResource("classpath:data-query-service-context.xml")
@EnableWebMvc
public class DataQueryServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(DataQueryServiceLauncher.class, args);
    }

}