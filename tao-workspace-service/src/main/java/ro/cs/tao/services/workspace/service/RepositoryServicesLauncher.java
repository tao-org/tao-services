package ro.cs.tao.services.workspace.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.commons.menu.Section;
import ro.cs.tao.services.commons.menu.ServiceEntry;
import ro.cs.tao.services.commons.menu.Visibility;
import ro.cs.tao.services.interfaces.RepositoryWatcherService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ImportResource("classpath:tao-workspace-service-context.xml")

public class RepositoryServicesLauncher extends ServiceLauncher {

    @Autowired
    RepositoryWatcherService repositoryWatcherService;

    public static void main(String[] args) {
        SpringApplication.run(RepositoryServicesLauncher.class, args);
    }

    public RepositoryServicesLauncher() {
        super();
    }

    @PostConstruct
    private void initialize() { repositoryWatcherService.startWatching(); }

    @PreDestroy
    private void stopWatcher() {
        repositoryWatcherService.stopWatching();
    }

    @Override
    public String serviceName() { return "Workspace Service"; }

    @Override
    protected List<ServiceEntry> exportedEntries() {
        final List<ServiceEntry> entries = new ArrayList<>();
        entries.add(new ServiceEntry("Repositories", Visibility.USER, Section.DATASETS, 3));
        entries.add(new ServiceEntry("Remote Web Services", Visibility.ADMIN, Section.ADMINISTRATION, 5));
        return entries;
    }
}
