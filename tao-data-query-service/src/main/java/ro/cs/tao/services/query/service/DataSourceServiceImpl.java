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
package ro.cs.tao.services.query.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.*;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.persistence.DataSourceConfigurationProvider;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.persistence.ContainerProvider;
import ro.cs.tao.persistence.DataSourceCredentialsProvider;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.model.datasource.DataSourceDescriptor;
import ro.cs.tao.utils.Crypto;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.async.Parallel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Cosmin Cara
 */
@Service("dataSourceService")
public class DataSourceServiceImpl implements DataSourceService {
    private static final Pattern satelliteVersionPattern = Pattern.compile("[-_]\\d[A-D]");
    @Autowired
    private DownloadListener downloadListener;

    @Autowired
    private EOProductProvider productProvider;

    @Autowired
    private DataSourceCredentialsProvider credentialsProvider;

    @Autowired
    private DataSourceConfigurationProvider configurationProvider;

    @Autowired
    private ContainerProvider containerProvider;

    @Override
    public SortedSet<String> getSupportedSensors() {
        return DataSourceManager.getInstance().getSupportedSensors();
    }

    @Override
    public List<String> getDatasourcesForSensor(String sensorName) {
        return DataSourceManager.getInstance().getNames(sensorName);
    }

    @Override
    public Map<String, String[]> getDataSourceProviders() {
        Map<String, String[]> instances = new HashMap<>();
        final DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        final Set<DataSource<?, ?>> dataSources = dataSourceManager.getRegisteredDataSources();
        for (DataSource<?, ?> dataSource : dataSources) {
            instances.put(dataSource.getId(), dataSource.getSupportedSensors());
        }
        return instances;
    }

    @Override
    public List<DataSourceDescriptor> getDatasourceInstances() {
        List<DataSourceDescriptor> instances = new ArrayList<>();
        final DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        //final SortedSet<String> sensors = dataSourceManager.getSupportedSensors();
        final SortedSet<String> sensors = getSupportedSensors();
        final List<DataSourceCredentials> userCredentials = credentialsProvider.getByUser(SessionStore.currentContext().getPrincipal().getName());
        for (String sensor : sensors) {
            //dataSourceManager.getNames(sensor).forEach(n -> {
            getDatasourcesForSensor(sensor).forEach(n -> {
                if (!"Local Database".equals(n)) {
                    DataSource<?, ?> dataSource = dataSourceManager.get(sensor, n);
                    Map<String, CollectionDescription> sensorTypes = dataSource.getSensorTypes();
                    if (sensorTypes != null) {
                        final Map<String, DataSourceParameter> parameters = dataSourceManager.getSupportedParameters(sensor, n);
                        CollectionDescription cd = sensorTypes.get(sensor);
                        final DataSourceDescriptor descriptor;
                        if (cd != null) {
                            String mission = cd.getMission();
                            if (mission == null) {
                                mission = sensor.indexOf('(') > 0 ? sensor.substring(0, sensor.indexOf('(')).trim() : sensor;
                            }
                            if (satelliteVersionPattern.matcher(mission).find()) {
                                mission = mission.substring(0, mission.length() - 1);
                            }
                            descriptor = new DataSourceDescriptor(mission, sensor, n, cd.getSensorType().friendlyName(),
                                                                  cd.getDescription(), cd.getTemporalCoverage(), cd.getSpatialCoverage(),
                                                                  parameters, dataSource.requiresAuthentication());
                        } else {
                            descriptor = new DataSourceDescriptor(sensor, sensor, n, SensorType.UNKNOWN.friendlyName(), "", "", "",
                                                                  parameters, dataSource.requiresAuthentication());
                        }
                        instances.add(descriptor);
                        //final DataSourceCredentials dsCreds = userCredentials.stream().filter(c -> c.getDataSource().equals(sensor + "-" + n)).findFirst().orElse(null);
                        final DataSourceCredentials dsCreds = userCredentials.stream().filter(c -> c.getDataSource().equals(n)).findFirst().orElse(null);
                        if (dsCreds != null) {
                            descriptor.setUser(dsCreds.getUserName());
                            descriptor.setPwd(dsCreds.getPassword());
                            if (dsCreds.getParams() != null) {
                                final DataSourceParameter uParam = parameters.values().stream()
                                        .filter(p -> p.getName().toLowerCase().contains("username")).findFirst().orElse(null);
                                if (uParam != null) {
                                    uParam.setDefaultValue(dsCreds.getParams().get(uParam.getName()));
                                    parameters.values().stream()
                                            .filter(p -> p.getName().toLowerCase().contains("password"))
                                            .findFirst()
                                            .ifPresent(pParam -> pParam.setDefaultValue(Crypto.decrypt(dsCreds.getParams().get(pParam.getName()),
                                                                                                       dsCreds.getParams().get(uParam.getName()))));
                                }
                            }
                        }
                    }
                }/* else {
                    instances.add(new DataSourceDescriptor(sensor, n, SensorType.UNKNOWN.friendlyName(), "Local database repository", "" , "",
                            dataSourceManager.getSupportedParameters(sensor, n)));
                }*/
            });
        }
        return instances;
    }

    @Override
    public List<DataSourceParameter> getSupportedParameters(String sensorName, String dataSourceName) {
        List<DataSourceParameter> parameters = null;
        DataSource<?, ?> dataSource = DataSourceManager.getInstance().get(sensorName, dataSourceName);
        if (dataSource != null) {
            Map<String, Map<String, ro.cs.tao.datasource.param.DataSourceParameter>> map = dataSource.getSupportedParameters();
            Map<String, ro.cs.tao.datasource.param.DataSourceParameter> parameterDescriptorMap = map.get(sensorName);
            if (parameterDescriptorMap != null) {
                parameters = new ArrayList<>(parameterDescriptorMap.values());
            }
        }
        return parameters;
    }

    @Override
    public long count(Query queryObject) throws SerializationException {
        return Query.toDataQuery(queryObject).getCount();
    }

    @Override
    public List<EOProduct> query(Query queryObject) throws SerializationException {
        if (StringUtils.isNotEmpty(queryObject.getUser()) && StringUtils.isNotEmpty(queryObject.getPassword())) {
            final String user = SessionStore.currentContext().getPrincipal().getName();
            //final String dsId = queryObject.getSensor() + "-" + queryObject.getDataSource();
            final String dsId = queryObject.getDataSource();
            DataSourceCredentials credentials = credentialsProvider.get(user, dsId);
            if (credentials == null) {
                credentials = new DataSourceCredentials();
                credentials.setDataSource(dsId);
                credentials.setUserId(user);
            }
            credentials.setUserName(queryObject.getUser());
            credentials.setPassword(queryObject.getPassword());
            final Map.Entry<String, String> additionalUser = queryObject.getValues().entrySet().stream().filter(e -> e.getKey().toLowerCase().contains("username")).findFirst().orElse(null);
            if (additionalUser != null) {
                final Map.Entry<String, String> additionalPwd = queryObject.getValues().entrySet().stream().filter(e -> e.getKey().toLowerCase().contains("password")).findFirst().orElse(null);
                if (additionalPwd != null) {
                    Map<String, String> params = new HashMap<>();
                    params.put(additionalUser.getKey(), additionalUser.getValue());
                    params.put(additionalPwd.getKey(), Crypto.encrypt(additionalPwd.getValue(), additionalUser.getValue()));
                    credentials.setParams(params);
                }
            }
            try {
                credentialsProvider.save(credentials);
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
        }
        final DataQuery dataQuery = Query.toDataQuery(queryObject);
        dataQuery.setQueryDelay(1);
        return dataQuery.execute();
    }

    @Override
    public List<EOProduct> fetch(Query query, final List<EOProduct> products, FetchMode mode, String localPath, String pathFormat) {
        final List<EOProduct> results;
        if (products != null) {
            final DataSourceComponent dsComponent = new DataSourceComponent(query.getSensor(), query.getDataSource());
            if (!StringUtilities.isNullOrEmpty(query.getUserId())) {
                dsComponent.setPrincipal(query::getUserId);
            }
            dsComponent.setUserCredentials(query.getUser(), query.getPassword());
            final DataSourceConfiguration dataSourceConfiguration = configurationProvider.get(dsComponent.getId());
            String localRepositoryRoot;
            final Properties properties = new Properties();
            if (dataSourceConfiguration != null) {
                dsComponent.setFetchMode(dataSourceConfiguration.getFetchMode());
                localRepositoryRoot = dataSourceConfiguration.getLocalRepositoryPath();
                Map<String, String> parameters = dataSourceConfiguration.getParameters();
                if (parameters != null) {
                    properties.putAll(parameters);
                }
            } else {
                localRepositoryRoot = ConfigurationManager.getInstance().getValue(String.format("local.%s.path", query.getSensor()));
                if (localRepositoryRoot == null) {
                    localRepositoryRoot = localPath;
                }
                dsComponent.setFetchMode(mode);
            }
            dsComponent.setProductStatusListener(downloadListener);
            boolean createSubfolder = Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("fetch.products.in.subfolders", "true"));
            String path = SystemVariable.USER_WORKSPACE.value();
            if (createSubfolder) {
                String subfolder = StringUtils.replaceEach(query.getLabel(),
                                                           new String[] { "/", "<", ">", "\\", "|", ":", "&", ";", " ", "?", "*" },
                                                           new String[] { "_", "_", "_", "_", "_", "_", "_", "_", "_", "_", "_" });
                Path pPath = Paths.get(path).resolve(subfolder);
                try {
                    Files.createDirectories(pPath);
                    path = pPath.toString();
                } catch (Exception e) {
                    Logger.getLogger(DataSourceService.class.getName()).warning("Cannot create folder " + pPath);
                }
            }
            final Set<String> existingSet = new HashSet<>(productProvider.getExistingProductNames(products.stream().map(EOProduct::getName).toArray(String[]::new)));
            products.removeIf(p -> existingSet.contains(p.getName()) && mode != FetchMode.RESUME);
            final Logger logger = Logger.getLogger(DataSourceService.class.getName());
            for (EOProduct product : products) {
                product.setProductStatus(ProductStatus.QUERIED);
                product.addReference(query.getUser());
                try {
                    if (productProvider.exists(product.getId())) {
                        productProvider.update(product);
                    } else {
                        productProvider.save(product);
                    }
                } catch (PersistenceException e) {
                    logger.warning(String.format("Cannot persist product %s. Reason: %s",product.getName(), e.getMessage()));
                }
            }
            final int parallelism = DataSourceManager.getInstance().get(dsComponent.getSensorName(),
                                                                        dsComponent.getDataSourceName()).getMaximumAllowedTransfers();
            final String finalPath = path;
            if ((dsComponent.getFetchMode() == FetchMode.SYMLINK || dsComponent.getFetchMode() == FetchMode.COPY) && localRepositoryRoot != null) {
                if (pathFormat != null) {
                    properties.put("local.archive.path.format", pathFormat);
                }
                if (parallelism > 1) {
                    results = Collections.synchronizedList(new ArrayList<>());
                    final String root = localRepositoryRoot;
                    Parallel.For(0, products.size(), parallelism, (i) -> {
                        try {
                            DataSourceComponent clone = cloneComponent(dsComponent, downloadListener);
                            results.addAll(clone.doFetch(products.subList(i, i + 1), null, finalPath, root, properties));
                        } catch (Exception ex) {
                            logger.severe(ex.getMessage());
                        }
                    });
                } else {
                    results = dsComponent.doFetch(products, null, path, localRepositoryRoot, properties);
                }
            } else {
                properties.put("auto.uncompress", "true");
                if (parallelism > 1) {
                    results = Collections.synchronizedList(new ArrayList<>());
                    Parallel.For(0, products.size(), parallelism, (i) -> {
                        try {
                            DataSourceComponent clone = cloneComponent(dsComponent, downloadListener);
                            results.addAll(clone.doFetch(products.subList(i, i + 1), null, finalPath, null, properties));
                        } catch (Exception ex) {
                            logger.severe(ex.getMessage());
                        }
                    });
                } else {
                    results = dsComponent.doFetch(products, null, path, null, properties);
                }
            }
        } else {
            results = null;
        }
        return results;
    }

    private DataSourceComponent cloneComponent(DataSourceComponent source, ProductStatusListener listener) throws CloneNotSupportedException {
        DataSourceComponent clone = source.clone();
        clone.setUserCredentials(source.getUserName(), source.getPassword());
        clone.setFetchMode(source.getFetchMode());
        clone.setProductStatusListener(listener);
        return clone;
    }

    @Override
    public DataSourceCredentials getDataSourceCredentials(String dataSource) {
        final List<DataSourceCredentials> userCredentials = credentialsProvider.getByUser(SessionStore.currentContext().getPrincipal().getName());
        final DataSourceCredentials dsCreds = userCredentials.stream().filter(c -> c.getDataSource().equals(dataSource)).findFirst().orElse(null);
        return dsCreds;
    }
}
