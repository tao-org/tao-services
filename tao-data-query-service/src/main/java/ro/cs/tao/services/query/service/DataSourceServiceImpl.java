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
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.*;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.persistence.DataSourceConfigurationProvider;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.gadm.AdminL1Unit;
import ro.cs.tao.gadm.Country;
import ro.cs.tao.persistence.*;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.serialization.GeometryAdapter;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.interfaces.SiteService;
import ro.cs.tao.services.model.datasource.DataSourceDescriptor;
import ro.cs.tao.utils.Crypto;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.RepositoryType;

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
    private static final Set<FetchMode> localFetchModes = EnumSet.of(FetchMode.COPY, FetchMode.SYMLINK, FetchMode.CHECK);
    private final Map<String, Map<String, DataSourceDescriptor>> cache = new HashMap<>();

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

    @Autowired
    private RepositoryProvider repositoryProvider;

    @Autowired
    private SiteService siteService;

    @Autowired
    private NamingRuleProvider namingRuleProvider;

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
    public String getProductTypeValue(String sensor, String productType, String dataSourceName) {
        Set<String> synonims = this.namingRuleProvider.getSynonims(productType);
        if (synonims == null || synonims.isEmpty()) {
            return null;
        }
        List<DataSourceParameter> parameters = getSupportedParameters(sensor, dataSourceName);
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        DataSourceParameter parameter = parameters.stream().filter(p -> CommonParameterNames.PRODUCT_TYPE.equals(p.getName())).findFirst().get();
        if (parameter != null) {
            Object[] valueSet = parameter.getValueSet();
            if (valueSet != null && valueSet.length > 0) {
                for (Object value : valueSet) {
                    if (synonims.contains(value.toString())) {
                        return value.toString();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<DataSourceDescriptor> getDatasourceInstances(String userId) {
        List<DataSourceDescriptor> instances = new ArrayList<>();
        final DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        //final SortedSet<String> sensors = dataSourceManager.getSupportedSensors();
        final SortedSet<String> sensors = getSupportedSensors();
        final List<DataSourceCredentials> userCredentials = credentialsProvider.getByUser(userId);
        for (String sensor : sensors) {
            //dataSourceManager.getNames(sensor).forEach(n -> {
            List<String> datasources = getDatasourcesForSensor(sensor);
            for (String n : datasources) {
                Map<String, DataSourceDescriptor> descriptorMap = cache.getOrDefault(sensor, new HashMap<>());
                if (!"Local Database".equals(n)) {
                    DataSourceDescriptor descriptor = descriptorMap.get(n);
                    if (descriptor == null) {
                        DataSource<?, ?> dataSource = dataSourceManager.get(sensor, n);
                        if (dataSource == null) {
                            Logger.getLogger(DataSourceService.class.getName()).warning("Datasource " + n + " for " + sensor + " doesn't appear to be registered");
                            continue;
                        }
                        Map<String, CollectionDescription> sensorTypes = dataSource.getSensorTypes();
                        if (sensorTypes != null) {
                            final Map<String, DataSourceParameter> parameters = dataSourceManager.getSupportedParameters(sensor, n);
                            final DataSourceConfiguration dataSourceConfiguration = configurationProvider.get(sensor + "-" + n);
                            // In case of symlink, copy or check fetch modes, authentication is not required
                            final boolean requiresAuthentication;
                            if (dataSourceConfiguration != null && localFetchModes.contains(dataSourceConfiguration.getFetchMode())) {
                                requiresAuthentication = false;
                            } else {
                                requiresAuthentication = dataSource.requiresAuthentication();
                            }
                            CollectionDescription cd = sensorTypes.get(sensor);
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
                                                                      parameters, requiresAuthentication,
                                                                      dataSource.is2FAEnabled() & requiresAuthentication,
                                                                      cd.isTokenNeeded() & requiresAuthentication);
                            } else {
                                descriptor = new DataSourceDescriptor(sensor, sensor, n, SensorType.UNKNOWN.friendlyName(), "", "", "",
                                                                      parameters, requiresAuthentication,
                                                                      dataSource.is2FAEnabled() & requiresAuthentication,
                                                                      false);
                            }
                            descriptorMap.put(n, descriptor);
                            final DataSourceCredentials dsCreds = userCredentials.stream().filter(c -> c.getDataSource().equals(n)).findFirst().orElse(null);
                            if (dsCreds != null) {
                                descriptor.setUser(dsCreds.getUserName());
                                descriptor.setPwd(dsCreds.getPassword());
                                descriptor.setSecret(dsCreds.getSecret());
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
                    }
                    instances.add(descriptor);
                }
            }
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
        checkInterrupted();
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
            final Map.Entry<String, String> secret = queryObject.getValues().entrySet().stream().filter(e -> e.getKey().toLowerCase().contains("secret")).findFirst().orElse(null);
            if (secret != null) {
                credentials.setSecret(secret.getValue());
            }
            try {
                credentialsProvider.save(credentials);
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
        }
        final DataQuery dataQuery = Query.toDataQuery(queryObject);
        checkInterrupted();
        dataQuery.setQueryDelay(1);
        return dataQuery.execute();
    }

    private void checkInterrupted() throws SerializationException {
        if (Thread.currentThread().isInterrupted()) {
            throw new SerializationException("Datasource query was cancelled.");
        }
    }

    @Override
    public List<EOProduct> fetch(Query query, final List<EOProduct> products, FetchMode mode, String localPath, String pathFormat) {
        final List<EOProduct> results;
        if (products != null) {
            final DataSourceComponent dsComponent = new DataSourceComponent(query.getSensor(), query.getDataSource());
            boolean userIdPresent = !StringUtilities.isNullOrEmpty(query.getUserId());
            if (userIdPresent) {
                dsComponent.setPrincipal(new UserPrincipal(query.getUserId()));
            }
            dsComponent.setUserCredentials(query.getUser(), query.getPassword(), query.getSecret());
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
            // This may yield unexpected results i.e. mingling userId -> String path = SystemVariable.USER_WORKSPACE.value();
            String path = userIdPresent
                          ? repositoryProvider.getUserSystemRepositories(query.getUserId())
                                              .stream().filter(r -> RepositoryType.LOCAL.equals(r.getType()))
                                              .findFirst().get().root()
                          : SystemVariable.USER_WORKSPACE.value();
            if (createSubfolder) {
                String subfolder = StringUtils.replaceEach(query.getLabel(),
                                                           new String[] { "/", "<", ">", "\\", "|", ":", "&", ";", " ", "?", "*" },
                                                           new String[] { "_", "_", "_", "_", "_", "_", "_", "_", "_", "_", "_" });
                Path pPath = Paths.get(path).resolve(subfolder);
                try {
                    FileUtilities.createDirectories(pPath);
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
            if (localFetchModes.contains(dsComponent.getFetchMode()) && localRepositoryRoot != null) {
                if (pathFormat != null) {
                    properties.put("local.archive.path.format", pathFormat);
                }
                /*if (parallelism > 1) {
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
                } else {*/
                    results = dsComponent.doFetch(products, null, path, localRepositoryRoot, properties);
                //}
            } else {
                properties.put("auto.uncompress",
                               ConfigurationManager.getInstance().getValue("datasource.decompress.downloads", "true"));
                /*if (parallelism > 1) {
                    results = Collections.synchronizedList(new ArrayList<>());
                    Parallel.For(0, products.size(), parallelism, (i) -> {
                        try {
                            DataSourceComponent clone = cloneComponent(dsComponent, downloadListener);
                            results.addAll(clone.doFetch(products.subList(i, i + 1), null, finalPath, null, properties));
                        } catch (Exception ex) {
                            logger.severe(ex.getMessage());
                        }
                    });
                } else {*/
                    results = dsComponent.doFetch(products, null, path, null, properties);
                //}
            }
        } else {
            results = null;
        }
        return results;
    }

    private DataSourceComponent cloneComponent(DataSourceComponent source, ProductStatusListener listener) throws CloneNotSupportedException {
        DataSourceComponent clone = source.clone();
        clone.setUserCredentials(source.getUserName(), source.getPassword(), source.getSecret());
        clone.setFetchMode(source.getFetchMode());
        clone.setProductStatusListener(listener);
        return clone;
    }

    @Override
    public DataSourceCredentials getDataSourceCredentials(String dataSource) {
        final List<DataSourceCredentials> userCredentials = credentialsProvider.getByUser(SessionStore.currentContext().getPrincipal().getName());
        return userCredentials.stream().filter(c -> c.getDataSource().equals(dataSource)).findFirst().orElse(null);
    }

    @Override
    public String lookupNameArea(String name) {
        // first try a country or administrative unit
        String normalizedName = name;
        if (normalizedName != null) {
            normalizedName = normalizedName.toLowerCase();
            normalizedName = normalizedName.replace("county", "").replace("country", "").replace("region", "").trim();
        }
        Country country = this.siteService.getCountryByName(normalizedName);
        if (country != null) {
            return new GeometryAdapter().unmarshal(country.getExtent());
        }
        AdminL1Unit unit = this.siteService.getAdminL1UnitByName(normalizedName);
        if (unit != null) {
            return new GeometryAdapter().unmarshal(unit.getExtent());
        }
        final ToponymSearchCriteria criteria = new ToponymSearchCriteria();
        criteria.setQ(normalizedName);
        try {
            WebService.setUserName(ConfigurationManager.getInstance().getValue("geonames.user", "kraftek"));
            ToponymSearchResult result = WebService.search(criteria);
            final List<Toponym> toponyms = result.getToponyms();
            if (toponyms != null && !toponyms.isEmpty()) {
                double lat = toponyms.get(0).getLatitude();
                double lon = toponyms.get(0).getLongitude();
                Polygon2D polygon = new Polygon2D();
                polygon.append(lon - 0.1, lat + 0.1);
                polygon.append(lon + 0.1, lat + 0.1);
                polygon.append(lon + 0.1, lat - 0.1);
                polygon.append(lon - 0.1, lat - 0.1);
                polygon.append(lon - 0.1, lat + 0.1);
                return polygon.toWKT(4);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DataSourceDescriptor createDescriptor(DataSourceManager dataSourceManager, List<DataSourceCredentials> userCredentials,
                                                  String sensor, String dataSourceName) {
        DataSource<?, ?> dataSource = dataSourceManager.get(sensor, dataSourceName);
        Map<String, CollectionDescription> sensorTypes = dataSource.getSensorTypes();
        DataSourceDescriptor descriptor = null;
        if (sensorTypes != null) {
            final Map<String, DataSourceParameter> parameters = dataSourceManager.getSupportedParameters(sensor, dataSourceName);
            final DataSourceConfiguration dataSourceConfiguration = configurationProvider.get(sensor + "-" + dataSourceName);
            // In case of symlink, copy or check fetch modes, authentication is not required
            final boolean requiresAuthentication;
            if (dataSourceConfiguration != null && localFetchModes.contains(dataSourceConfiguration.getFetchMode())) {
                requiresAuthentication = false;
            } else {
                requiresAuthentication = dataSource.requiresAuthentication();
            }
            CollectionDescription cd = sensorTypes.get(sensor);
            if (cd != null) {
                String mission = cd.getMission();
                if (mission == null) {
                    mission = sensor.indexOf('(') > 0 ? sensor.substring(0, sensor.indexOf('(')).trim() : sensor;
                }
                if (satelliteVersionPattern.matcher(mission).find()) {
                    mission = mission.substring(0, mission.length() - 1);
                }
                descriptor = new DataSourceDescriptor(mission, sensor, dataSourceName, cd.getSensorType().friendlyName(),
                                                      cd.getDescription(), cd.getTemporalCoverage(), cd.getSpatialCoverage(),
                                                      parameters, requiresAuthentication,
                                                      dataSource.is2FAEnabled() & requiresAuthentication,
                                                      cd.isTokenNeeded() & requiresAuthentication);
            } else {
                descriptor = new DataSourceDescriptor(sensor, sensor, dataSourceName, SensorType.UNKNOWN.friendlyName(), "", "", "",
                                                      parameters, requiresAuthentication,
                                                      dataSource.is2FAEnabled() & requiresAuthentication,
                                                      false);
            }
            final DataSourceCredentials dsCreds = userCredentials.stream().filter(c -> c.getDataSource().equals(dataSourceName)).findFirst().orElse(null);
            if (dsCreds != null) {
                descriptor.setUser(dsCreds.getUserName());
                descriptor.setPwd(dsCreds.getPassword());
                descriptor.setSecret(dsCreds.getSecret());
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
        return descriptor;
    }
}
