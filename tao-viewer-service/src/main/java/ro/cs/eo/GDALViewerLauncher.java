package ro.cs.eo;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.services.commons.ServiceLauncher;

@Configuration
@ImportResource("classpath:viewer-service-context.xml")
@EnableWebMvc
public class GDALViewerLauncher implements ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(GDALViewerLauncher.class, args);
    }

    @Override
    public String serviceName() { return "GDAL Viewer Service"; }
}
