package ro.cs.tao.services.scheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;

@Configuration
@ImportResource("classpath:tao-scheduling-services-context.xml")

public class SchedulingServiceLauncher implements ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(SchedulingServiceLauncher.class, args);
    }

	@Override
	public String serviceName() { return "Scheduling service";}

}
