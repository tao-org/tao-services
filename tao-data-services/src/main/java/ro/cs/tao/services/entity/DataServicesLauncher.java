package ro.cs.tao.services.entity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication()
@ImportResource({"classpath:tao-data-services-context.xml", "classpath*:tao-persistence-context.xml" })
public class DataServicesLauncher {

    public static void main(String[] args) {
        SpringApplication.run(DataServicesLauncher.class, args);
    }

}
