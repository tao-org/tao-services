package ro.cs.eo;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;

@Configuration
@ImportResource("classpath:viewer-service-context.xml")

public class GDALViewerLauncher implements ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(GDALViewerLauncher.class, args);
    }

    @Override
    public String serviceName() { return "GDAL Viewer Service"; }
}
