package ro.cs.tao.services.entity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.interfaces.WorkspaceService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@EnableScheduling
@Configuration
@ImportResource("classpath:tao-workspace-service-context.xml")
@EnableWebMvc
@EnableAsync
public class WorkspaceServicesLauncher implements ServiceLauncher {

    @Autowired
    WorkspaceService workspaceService;

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceServicesLauncher.class, args);
    }

    @PostConstruct
    private void initialize() {
        workspaceService.startWatching();
    }

    @PreDestroy
    private void updateUsersInputQuota() {
        workspaceService.stopWatching();
    }

    @Override
    public String serviceName() { return "Workspace Service"; }
}
