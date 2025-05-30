package ro.cs.tao.services.scheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.commons.menu.Section;
import ro.cs.tao.services.commons.menu.ServiceEntry;
import ro.cs.tao.services.commons.menu.Visibility;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ImportResource("classpath:tao-scheduling-services-context.xml")

public class SchedulingServiceLauncher extends ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(SchedulingServiceLauncher.class, args);
    }

    public SchedulingServiceLauncher() {
        super();
    }

    @Override
	public String serviceName() { return "Scheduling service";}

    @Override
    protected List<ServiceEntry> exportedEntries() {
        final List<ServiceEntry> entries = new ArrayList<>();
        entries.add(new ServiceEntry("Scheduled Jobs", "my/scheduling", "./fragments/scheduling-admin2.fragment.html", Visibility.USER, Section.WORKFLOWS, 3));
        return entries;
    }
}
