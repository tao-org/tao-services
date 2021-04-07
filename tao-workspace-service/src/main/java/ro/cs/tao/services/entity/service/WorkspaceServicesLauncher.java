package ro.cs.tao.services.entity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.persistence.repository.UserRepository;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.interfaces.WorkspaceService;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@EnableScheduling
@Configuration
@ImportResource("classpath:tao-workspace-service-context.xml")
@EnableWebMvc
public class WorkspaceServicesLauncher implements ServiceLauncher {

    @Autowired
    WorkspaceService workspaceService;

    @Autowired
    UserRepository userRepository;

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceServicesLauncher.class, args);
    }

    @PostConstruct
    private void initialize() {
        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        if (activeUsers != null) {
            final Path rootPath = Paths.get(SystemVariable.ROOT.value());
            for (User user : activeUsers) {
                if (!SystemPrincipal.instance().getName().equals(user.getUsername())) {
                    Path userWorkspace = rootPath.resolve(user.getUsername());
                    try {
                        workspaceService.walkAndRegisterDirectories(userWorkspace);
                    } catch (IOException e) {
                        System.out.println(String.format("Failed to watch user workspace [user=%s, reason=%s]",
                                user.getUsername(), e.getMessage()));
                    }
                }
            }
        }
    }

    @Scheduled(fixedDelay = 50000)
    public void enableWorkspaceWatcher() {
        System.out.println("alive :)");
        System.out.println("Fixed delay task - " + System.currentTimeMillis() / 1000);
        if(workspaceService.hasKeys()){
            workspaceService.watch();
        }
    }

    @Override
    public String serviceName() { return "Workspace Service"; }
}
