package ro.cs.tao.services.startup;

public class ContainerRegistrar extends BaseLifeCycle {
    @Override
    public int priority() {
        return 5;
    }

    @Override
    public void onStartUp() {
        /*List<DockerImageInstaller> installers = TopologyManager.getInstance().getInstallers();
        if (installers != null && installers.size() > 0) {
            logger.finest(String.format("Found %d docker image plugins: %s", installers.size(),
                                        installers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.joining(","))));
            for (DockerImageInstaller imageInstaller : installers) {
                try {
                    Container container = imageInstaller.installImage();
                    if (container != null && container.getLogo() != null) {
                        Path imgPath = TaoServicesStartup.homeDirectory().resolve("static").resolve("workflow").resolve("media")
                                .resolve(container.getId() + ".png");
                        if (!Files.exists(imgPath)) {
                            Files.createDirectories(imgPath.getParent());
                            Files.write(imgPath, Base64.getDecoder().decode(container.getLogo()));
                        }
                    }
                } catch (Throwable e) {
                    logger.severe(e.getMessage());
                }
            }
        } else {
            logger.fine("No docker image plugin found");
        }
        final List<StandaloneContainer> containers = TopologyManager.getInstance().getStandaloneContainers();
        if (containers != null && containers.size() > 0) {
            logger.finest(String.format("Found %d docker standalone containers: %s", containers.size(),
                                        containers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.joining(","))));
            for (StandaloneContainer container : containers) {
                try {
                    container.install();
                    logger.fine(String.format("Installed docker image %s", container.getContainerName()));
                    container.start();
                    logger.fine(String.format("Started docker container %s", container.getContainerName()));
                } catch (Throwable e) {
                    logger.severe(e.getMessage());
                }
            }
        } else {
            logger.fine("No docker standalone container found");
        }*/
    }

    @Override
    public void onShutdown() {

    }
}
