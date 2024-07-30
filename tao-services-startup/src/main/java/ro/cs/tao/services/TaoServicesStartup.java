/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services;

import org.ggf.drmaa.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.transaction.PlatformTransactionManager;
import ro.cs.tao.component.RuntimeOptimizer;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.eodata.OutputDataHandler;
import ro.cs.tao.eodata.metadata.MetadataInspector;
import ro.cs.tao.execution.Executor;
import ro.cs.tao.execution.model.ResourceUsage;
import ro.cs.tao.execution.model.ResourceUsageReport;
import ro.cs.tao.execution.model.TaskSelector;
import ro.cs.tao.execution.persistence.ResourceUsageProvider;
import ro.cs.tao.execution.persistence.ResourceUsageReportProvider;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.messaging.system.StartupCompletedMessage;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.TransactionalMethod;
import ro.cs.tao.quota.QuotaManager;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.StartupBase;
import ro.cs.tao.services.interfaces.LogoutListener;
import ro.cs.tao.services.startup.LifeCycleProcessor;
import ro.cs.tao.services.startup.LifeCycleProcessorListener;
import ro.cs.tao.spi.OutputDataHandlerManager;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.topology.docker.DockerImageInstaller;
import ro.cs.tao.user.LogEvent;
import ro.cs.tao.user.SessionDuration;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.executors.FileProcessFactory;
import ro.cs.tao.utils.executors.MemoryUnit;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Cosmin Cara
 */

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
@EnableScheduling

public class TaoServicesStartup extends StartupBase implements LifeCycleProcessorListener {
    @Autowired
    private LifeCycleProcessor lifeCycleProcessor;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private AuditProvider auditProvider;
    @Autowired
    private ResourceUsageProvider resourceUsageProvider;
    @Autowired
    private ResourceUsageReportProvider resourceUsageReportProvider;

    private final static Logger logger = Logger.getLogger(TaoServicesStartup.class.getName());
    private static final Map<String, Class> plugins = new LinkedHashMap<String, Class>() {{
        put("Docker plugins", DockerImageInstaller.class);
        put("Datasource plugins", DataSource.class);
        put("DRMAA plugins", SessionFactory.class);
        put("Executor plugins", Executor.class);
        put("Metadata plugins", MetadataInspector.class);
        put("Product plugins", OutputDataHandler.class);
        put("Runtime plugins", RuntimeOptimizer.class);
        put("Orchestrator plugins", TaskSelector.class);
        put("Quota Manager plugins", QuotaManager.class);
    }};

    public static void main(String[] args) throws IOException {
        run(TaoServicesStartup.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            final ConfigurationProvider cfgManager = ConfigurationManager.getInstance();
            if (cfgManager == null) {
                throw new RuntimeException("Framework configuration unavailable");
            }
            cfgManager.setValue(ConfigurationProvider.APP_HOME, homeDirectory().toString());
            try {
                FileUtilities.ensureExists(Paths.get(SystemVariable.SHARED_WORKSPACE.value()));
                FileUtilities.ensureExists(Paths.get(SystemVariable.SHARED_FILES.value()));
                String tempPath = cfgManager.getValue("spring.servlet.multipart.location", "/mnt/tao/tmp");
                FileUtilities.ensureExists(Paths.get(tempPath));
                Path cachePath = TaoServicesStartup.homeDirectory().resolve("static").resolve("previews");
                FileUtilities.ensureExists(cachePath);
            } catch (IOException e) {
                logger.severe("Cannot create required folders: " + e.getMessage());
                System.exit(1);
            }
            TransactionalMethod.setTransactionManager(transactionManager);
            lifeCycleProcessor.activate(this);
        } else if (event instanceof HttpSessionDestroyedEvent) {
            final LogEvent evt = new LogEvent();
            final HttpSession session = ((HttpSessionDestroyedEvent) event).getSession();
            SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            // There may be initial sessions that don't have a security context. Ignore them.
            if (context != null && context.getAuthentication() != null && context.getAuthentication().getPrincipal() != null) {
                final String userId = ((Principal) context.getAuthentication().getPrincipal()).getName();
                if (userId == null) {
                    return;
                }
                evt.setUserId(userId);
                evt.setTimestamp(LocalDateTime.now());
                evt.setEvent("Logout");
                try {
                    auditProvider.save(evt);
                    final Set<LogoutListener> listeners = ServiceRegistryManager.getInstance().getServiceRegistry(LogoutListener.class).getServices();
                    if (listeners != null) {
                        for (LogoutListener listener : listeners) {
                            final SessionDuration sessionDuration = auditProvider.getLastUserSession(userId);
                            final ResourceUsageReport report = resourceUsageReportProvider.getByUserId(userId);
                            int processingTime = 0;
                            final List<ResourceUsage> usages;
                            if (report != null) {
                                usages = resourceUsageProvider.getByUserIdSince(userId, report.getLastReportTime());
                            } else {
                                usages = resourceUsageProvider.getByUserId(userId);
                            }
                            if (usages != null) {
                                for (ResourceUsage usage : usages) {
                                    processingTime += (int) Duration.between(usage.getStartTime(),
                                                                             usage.getEndTime() != null
                                                                                ? usage.getEndTime()
                                                                                : LocalDateTime.now()).toSeconds();
                                }
                            }
                            listener.doAction(userId, BaseController.tokenOf(userId), sessionDuration, processingTime);
                        }
                        BaseController.clearToken(userId);
                    }
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            }
        }
    }

    @Override
    public void activationCompleted() {
        for (Map.Entry<String, Class> entry : plugins.entrySet()) {
            ServiceRegistry<?> registry = ServiceRegistryManager.getInstance().getServiceRegistry(entry.getValue());
            Set<?> instances = registry.getServices();
            logger.info(String.format("Installed %s: %s",
                                      entry.getKey(), instances.stream().map(i -> i.getClass().getSimpleName())
                                                               .sorted().collect(Collectors.joining(","))));
        }
        OutputDataHandlerManager.getInstance().setFileProcessFactory(FileProcessFactory.createLocal());
        final ConfigurationProvider cfgProvider = ConfigurationManager.getInstance();
        final String url = cfgProvider.getValue("tao.services.base");
        final String path = cfgProvider.getValue("site.path");
        Path configJsPath = Paths.get(path).resolve("assets").resolve("dist").resolve("js").resolve("config.js");
        try (Stream<String> stream = Files.lines(configJsPath)) {
            final List<String> lines = stream.map(line -> {
                if (line.contains("baseWssUrl") &&
                        (!line.contains("ws:") || !line.contains("wss:"))) {
                    return line.substring(0, line.indexOf("'") + 1) +
                            (url.endsWith("/") ? url : url + "/").replace("http", "ws") +
                            line.substring(line.lastIndexOf("'"));
                } else if (line.contains("openStackPresent")) {
                    return line.substring(0, line.indexOf("=") + 1) +
                            TopologyManager.getInstance().isExternalProviderAvailable() + ";";
                } else if (line.contains("baseRestApiURL") &&
                        (!line.contains("http:") || !line.contains("https:"))) {
                    return line.substring(0, line.indexOf("'") + 1) +
                            (url.endsWith("/") ? url : url + "/") +
                            line.substring(line.lastIndexOf("'"));
                } else {
                    return line;
                }
            }).collect(Collectors.toList());
            Files.write(configJsPath, lines);
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        logger.info(String.format("Web interface is accessible at %s", url));
        Messaging.send(SystemPrincipal.instance(), Topic.SYSTEM.value(), new StartupCompletedMessage());
    }

    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.MINUTES)
    public void cleanupPreviewCache() {
        String sitePath = ConfigurationManager.getInstance().getValue("site.path");
        if (sitePath == null || sitePath.isEmpty()) {
            logger.warning("Cannot determine site path");
            return;
        }
        String stringSize = ConfigurationManager.getInstance().getValue("preview.cache.size", "20MB").trim();
        String unit = stringSize.substring(stringSize.length() - 2);
        final long limit = Long.parseLong(stringSize.replace(unit, "").trim()) * unitSize(unit);
        final Path cachePath = Paths.get(sitePath).resolve("previews");
        try {
            long size = FileUtilities.folderSize(cachePath);
            try (Stream<Path> stream = Files.list(cachePath)) {
                List<Path> files = stream.sorted((o1, o2) -> {
                    try {
                        return Files.getLastModifiedTime(o2).compareTo(Files.getLastModifiedTime(o1));
                    } catch (IOException e) {
                        return 0;
                    }
                }).collect(Collectors.toList());
                while (size >= limit) {
                    Path file = files.get(files.size() - 1);
                    size -= Files.size(file);
                    Files.delete(file);
                    files.remove(files.size() - 1);
                }
            }
        } catch (IOException e) {
            logger.warning("Exception encountered while cleaning up cache: " + e.getMessage());
        }
    }

    private long unitSize(String unit) {
        try {
            return MemoryUnit.getEnumConstantByName(unit).value();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unit not supported");
        }
    }
}
