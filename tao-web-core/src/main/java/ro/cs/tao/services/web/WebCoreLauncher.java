package ro.cs.tao.services.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ro.cs.tao.services.commons.ServiceLauncher;

@SpringBootApplication()
public class WebCoreLauncher extends ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(WebCoreLauncher.class, args);
    }

    @Override
    public String serviceName() {
        return "Web UI Core";
    }
}
