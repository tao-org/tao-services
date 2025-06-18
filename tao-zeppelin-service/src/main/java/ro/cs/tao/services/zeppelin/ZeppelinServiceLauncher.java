package ro.cs.tao.services.zeppelin;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;

/**
 * @author Efrem-Hagiu Stefan
 */
@Configuration
@ImportResource("classpath:zeppelin-service-context.xml")
public class ZeppelinServiceLauncher extends ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(ZeppelinServiceLauncher.class, args);
    }

    public ZeppelinServiceLauncher() {
        super();
    }

    @Override
    public String serviceName() { return "Apache Zeppelin Helper Service"; }
}


