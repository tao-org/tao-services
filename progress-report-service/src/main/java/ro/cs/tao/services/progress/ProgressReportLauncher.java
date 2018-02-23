package ro.cs.tao.services.progress;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ImportResource("classpath:progress-report-service-context.xml")
@EnableWebMvc
public class ProgressReportLauncher {
    public static void main(String[] args) {
        SpringApplication.run(ProgressReportLauncher.class, args);
    }
}
