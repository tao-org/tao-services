package ro.cs.tao.services.simpipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;

/**
 * @author Efrem-Hagiu Stefan
 */
@SpringBootApplication()
@ImportResource({"classpath:tao-simpipe-service-context.xml"})
public class SimpipeLauncher implements ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(SimpipeLauncher.class, args);
    }

    @Override
    public String serviceName() { return "Simpipe Service"; }
}