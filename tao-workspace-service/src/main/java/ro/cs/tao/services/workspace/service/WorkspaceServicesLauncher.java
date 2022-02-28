package ro.cs.tao.services.workspace.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.interfaces.WorkspaceWatcherService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
@ImportResource("classpath:tao-workspace-service-context.xml")
@EnableWebMvc
public class WorkspaceServicesLauncher implements ServiceLauncher {

    @Autowired
    WorkspaceWatcherService workspaceWatcherService;

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceServicesLauncher.class, args);
    }

    @PostConstruct
    private void initialize() { workspaceWatcherService.startWatching(); }

    @PreDestroy
    private void stopWatcher() {
        workspaceWatcherService.stopWatching();
    }

    @Override
    public String serviceName() { return "Workspace Service"; }
}
