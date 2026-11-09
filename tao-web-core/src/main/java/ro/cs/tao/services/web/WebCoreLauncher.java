package ro.cs.tao.services.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.commons.menu.Section;
import ro.cs.tao.services.commons.menu.ServiceEntry;
import ro.cs.tao.services.commons.menu.Visibility;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication()
public class WebCoreLauncher extends ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(WebCoreLauncher.class, args);
    }

    @Override
    public String serviceName() {
        return "Web UI Core";
    }

    @Override
    protected List<ServiceEntry> exportedEntries() {
        final List<ServiceEntry> entries = new ArrayList<>();
        entries.add(new ServiceEntry("Dashboard", "admin/dashboard", "./fragments/dashboard-admin.fragment.html", new Visibility[] { Visibility.USER, Visibility.ADMIN }, Section.DASHBOARD, 1));
        return entries;
    }
}
