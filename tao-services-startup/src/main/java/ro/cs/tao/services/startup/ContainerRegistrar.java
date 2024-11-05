package ro.cs.tao.services.startup;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Container;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.topology.docker.DockerImageInstaller;
import ro.cs.tao.topology.docker.SingletonContainer;
import ro.cs.tao.utils.FileUtilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers the available docker plugins/containers.
 */
public class ContainerRegistrar extends BaseLifeCycle {
    @Override
    public int priority() {
        return 5;
    }

    @Override
    public void onStartUp() {
        final List<DockerImageInstaller> installers = TopologyManager.getInstance().getInstallers();
        final Set<String> dbContainers = persistenceManager.containers().list()
                                                           .stream()
                                                           .map(Container::getName)
                                                           .collect(Collectors.toSet());
        if (installers != null && !installers.isEmpty()) {
            installers.removeIf(i -> {
                final String[] tokens = i.getContainerName().split("/");
                return dbContainers.contains(tokens[tokens.length - 1]);
            });
            if (installers.isEmpty()) {
                logger.fine("No new docker image plugins found");
            } else {
                logger.finest(() -> String.format("Found %d new docker image plugins: %s", installers.size(),
                                                  installers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.joining(","))));
                for (DockerImageInstaller imageInstaller : installers) {
                    try {
                        Container container = imageInstaller.installImage();
                        if (container != null && container.getLogo() != null) {
                            Path imgPath = Paths.get(ConfigurationManager.getInstance().getValue("site.path"))
                                                .resolve("workflow").resolve("media")
                                                .resolve(container.getId() + ".png");
                            if (!Files.exists(imgPath)) {
                                FileUtilities.createDirectories(imgPath.getParent());
                                Files.write(imgPath, Base64.getDecoder().decode(container.getLogo().trim()));
                            }
                        }
                    } catch (Throwable e) {
                        logger.severe(e.getMessage());
                    }
                }
            }
        } else {
            logger.fine("No docker image plugin found");
        }
        final List<SingletonContainer> containers = TopologyManager.getInstance().getStandaloneContainers();
        if (containers != null && !containers.isEmpty()) {
            logger.finest(() -> String.format("Found %d docker standalone containers: %s", containers.size(),
                                              containers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.joining(","))));
            for (SingletonContainer container : containers) {
                try {
                    container.install();
                    logger.fine(() -> String.format("Installed docker image %s", container.getContainerName()));
                    if (!container.isPerUser()) {
                        container.start();
                        logger.fine(() -> String.format("Started docker container %s", container.getContainerName()));
                    }
                } catch (Throwable e) {
                    logger.severe(e.getMessage());
                }
            }
        } else {
            logger.fine("No docker standalone container found");
        }
    }

    @Override
    public void onShutdown() {
        final List<SingletonContainer> containers = TopologyManager.getInstance().getStandaloneContainers();
        if (containers != null && !containers.isEmpty()) {
            for (SingletonContainer container : containers) {
                try {
                    logger.fine(() -> String.format("Attempting to stop container %s", container.getContainerName()));
                    container.shutdown();
                    logger.fine(() -> String.format("Container %s stopped", container.getContainerName()));
                } catch (Throwable e) {
                    logger.severe(e.getMessage());
                }
            }
        }
    }
}
