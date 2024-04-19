package ro.cs.tao.services.workspace.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.interfaces.RepositoryWatcherService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
@ImportResource("classpath:tao-workspace-service-context.xml")

public class RepositoryServicesLauncher implements ServiceLauncher {

    @Autowired
    RepositoryWatcherService repositoryWatcherService;

    public static void main(String[] args) {
        SpringApplication.run(RepositoryServicesLauncher.class, args);
    }

    @PostConstruct
    private void initialize() { repositoryWatcherService.startWatching(); }

    @PreDestroy
    private void stopWatcher() {
        repositoryWatcherService.stopWatching();
    }

    @Override
    public String serviceName() { return "Workspace Service"; }
}
