package ro.cs.tao.services.startup;

import ro.cs.tao.Tag;
import ro.cs.tao.component.Identifiable;
import ro.cs.tao.component.NodeAffinity;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.datasource.stac.STACSource;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.persistence.PersistenceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Registers the data sources that were not previously registered.
 */
public class DataSourceRegistrar extends BaseLifeCycle {

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public void onStartUp() {
        SortedSet<String> sensors = DataSourceManager.getInstance().getSupportedSensors();
        if (sensors != null) {
            Set<String> existing = persistenceManager.dataSourceComponents().list()
                    .stream()
                    .filter(DataSourceComponent::getSystem)
                    .map(Identifiable::getId)
                    .collect(Collectors.toSet());
            List<Tag> tags = persistenceManager.tags().list(TagType.DATASOURCE);
            if (tags == null) {
                tags = new ArrayList<>();
            }
            String componentId;
            List<String> newDs = null;
            for (String sensor : sensors) {
                Tag sensorTag = tags.stream().filter(t -> t.getText().equalsIgnoreCase(sensor)).findFirst().orElse(null);
                if (sensorTag == null) {
                    try {
                        sensorTag = persistenceManager.tags().save(new Tag(TagType.DATASOURCE, sensor));
                        tags.add(sensorTag);
                    } catch (PersistenceException e) {
                        logger.severe(e.getMessage());
                    }
                }
                List<String> dsNames = DataSourceManager.getInstance().getNames(sensor);
                for (String dsName : dsNames) {
                    componentId = sensor + "-" + dsName;
                    if (!existing.contains(componentId)) {
                        Tag dsNameTag = tags.stream().filter(t -> t.getText().equalsIgnoreCase(dsName)).findFirst().orElse(null);
                        if (dsNameTag == null) {
                            try {
                                dsNameTag = persistenceManager.tags().save(new Tag(TagType.DATASOURCE, dsName));
                                tags.add(dsNameTag);
                            } catch (PersistenceException e) {
                                logger.severe(e.getMessage());
                            }
                        }
                        DataSourceComponent dataSourceComponent = new DataSourceComponent(sensor, dsName);
                        dataSourceComponent.setFetchMode(FetchMode.OVERWRITE);
                        dataSourceComponent.setLabel(sensor + " from " + dsName);
                        dataSourceComponent.setVersion("1.0");
                        dataSourceComponent.setDescription(dataSourceComponent.getId());
                        dataSourceComponent.setAuthors("TAO Team");
                        dataSourceComponent.setCopyright("(C) TAO Team");
                        dataSourceComponent.setNodeAffinity(NodeAffinity.Any);
                        dataSourceComponent.setSystem(true);
                        if (sensorTag != null) {
                            dataSourceComponent.addTags(sensorTag.getText());
                        }
                        if (dsNameTag != null) {
                            dataSourceComponent.addTags(dsNameTag.getText());
                        }
                        try {
                            dataSourceComponent = persistenceManager.dataSourceComponents().save(dataSourceComponent);
                            if (newDs == null) {
                                newDs = new ArrayList<>();
                            }
                            newDs.add(dataSourceComponent.getId());
                        } catch (PersistenceException e) {
                            logger.severe(e.getMessage());
                        }
                    }
                    existing.remove(componentId);
                }
            }
            if (newDs != null) {
                logger.finest(String.format("Registered %s new data source components", newDs.size()));
            }
            if (!existing.isEmpty()) {
                logger.warning(String.format("There are %s data source components in the database that have not been found: %s",
                                             existing.size(), String.join(",", existing)));
                try {
                    persistenceManager.dataSourceComponents().delete(existing);
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            }
            final List<Container> stacServices = persistenceManager.containers().getByType(ContainerType.STAC);
            if (stacServices != null && !stacServices.isEmpty()) {
                STACSource.setConfigurationProvider(persistenceManager.dataSourceConfigurationProvider());
                for (Container service : stacServices) {
                    DataSourceManager.getInstance().registerDataSource(new STACSource(service.getName()));
                }
                logger.finest(String.format("Registered %s STAC services", stacServices.size()));
            }
        }
    }

    @Override
    public void onShutdown() {

    }
}
