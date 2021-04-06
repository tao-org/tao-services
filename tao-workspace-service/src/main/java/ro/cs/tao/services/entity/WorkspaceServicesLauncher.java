package ro.cs.tao.services.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.interfaces.WorkspaceService;

@Configuration
@ImportResource("classpath:tao-workspace-service-context.xml")
@EnableWebMvc
public class WorkspaceServicesLauncher implements ServiceLauncher {

    @Autowired
    WorkspaceService workspaceService;

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceServicesLauncher.class, args);
    }

    @Scheduled(fixedDelay = 50000)
    public void enableWorkspaceWatcher() {
        workspaceService.watch();
    }

    @Override
    public String serviceName() { return "Workspace Service"; }
}
