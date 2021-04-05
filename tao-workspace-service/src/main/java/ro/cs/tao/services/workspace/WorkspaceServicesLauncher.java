package ro.cs.tao.services.workspace;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.services.commons.ServiceLauncher;

@Configuration
@ImportResource("classpath:tao-workspace-service-context.xml")
@EnableWebMvc
public class WorkspaceServicesLauncher implements ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceServicesLauncher.class, args);
    }

    @Override
    public String serviceName() { return "Workspace Service"; }
}
