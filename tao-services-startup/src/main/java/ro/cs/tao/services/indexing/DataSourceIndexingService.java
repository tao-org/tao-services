package ro.cs.tao.services.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.*;
import ro.cs.tao.datasource.db.DatabaseSource;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.IndexedDataSourceParameter;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.serialization.GeometryAdapter;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.utils.DateUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ro.cs.tao.serialization.JsonMapper.JSON_INDEXED_DATA_SOURCE_REFERENCE;

/**
 * Used for indexing existing data sources. Starts with a delay of 1 minute (delayMinutes) and it will pe run once a day
 *
 * @author dstefanescu
 */

@Service("dataSourceIndexingService")
public class DataSourceIndexingService {
    private static final String INDEXING_PARAMETER_DESCRIPTOR_RESOURCE = "config/indexSensor.json";
    private static final int PAGE_SIZE = 200;
    private static final int MAX_RECORDS = 200;
    private static Map<String, IndexedDataSourceParameter> readParamDescriptors = Collections.synchronizedMap(new HashMap<>());

    private static final Pattern temporalPattern = Pattern.compile("[Ff]rom (?:[A-Za-z ]*)(\\d{4})( to(?:[A-Za-z ]*)(\\d{4}|[Pp]resent))?");
    private static final Pattern acquisitionDatePattern = Pattern.compile("((?:[A-Za-z0-9_]*?(?=\\d{8}))((\\d{4})(\\d{2})(\\d{2}))?(?:[A-Za-z0-9_]*))\\.([A-Za-z.]+)");
    private static final int delayMinutes = 1;
    private static final int fixedDelayMinutes = 1440;
    private final Logger logger = Logger.getLogger(getClass().getName());

    // TODO: remove comment marks for PostContruct and Scheduled annotations
    //@PostConstruct
    public void onIndexingDataSourcesStartup() {
        logger.info(String.format("Startup indexing data sources task with %s delay and %s interval", delayMinutes, fixedDelayMinutes));
        readPropFiles();
    }

    //@Scheduled(fixedDelay = fixedDelayMinutes, initialDelay = delayMinutes, timeUnit = TimeUnit.MINUTES)
    public void scheduleIndexingDataSources() {
        logger.info("Indexing data sources");
        DatabaseSource source = new DatabaseSource();
        final DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        final Set<DataSource<?, ?>> dataSources = dataSourceManager.getRegisteredDataSources();
        LocalDateTime today = LocalDateTime.now();
        AtomicInteger dsKey = new AtomicInteger();
        for (DataSource dataSource : dataSources) {
            // ignore data sources that are not described in JSON file (local database and aws)
            if (readParamDescriptors.containsKey(dataSource.getId())) {
                String insertDsStatement = "INSERT INTO product.data_sources_index (data_source_name, sensor_name, " +
                        "from_year, to_year, last_run)" +
                        " VALUES(?,?,?,?,?) ON CONFLICT(data_source_name, sensor_name)" +
                        " DO UPDATE SET to_year=EXCLUDED.to_year, last_run=EXCLUDED.last_run";
                String insertIndexedDsStatement = "INSERT INTO product.data_sources_index_props (data_source_id, footprint, acquisition_date)" +
                        " VALUES(?,?,?) ON CONFLICT(data_source_id, footprint, acquisition_date)" +
                        " DO NOTHING";
                String updateDsParamsStatement = "UPDATE product.data_sources_index SET params=?,last_run=? WHERE data_source_name=? AND sensor_name=?";

                dataSource.getSensorTypes().forEach((key, value) -> {
                    List<IndexedDataSourceDescriptor> existingDataSources = new ArrayList<>();
                    try (Connection sqlConnection = source.getConnection();
                         PreparedStatement stmt = sqlConnection.prepareStatement("SELECT * FROM product.data_sources_index WHERE data_source_name='" + dataSource.getId() +
                                 "' AND sensor_name='" + key.toString() + "'");
                         ResultSet rs = stmt.executeQuery()) {
                        ObjectMapper mapper = new ObjectMapper();
                        while (rs.next()) {
                            IndexedDataSourceDescriptor dsDescriptor = new IndexedDataSourceDescriptor();
                            dsDescriptor.setDataSourceName(rs.getString("data_source_name"));
                            dsDescriptor.setSensorName(rs.getString("sensor_name"));
                            dsDescriptor.setToYear(rs.getInt("to_year"));
                            dsDescriptor.setFromYear(rs.getInt("from_year"));
                            if (rs.getTimestamp("last_run") != null) {
                                dsDescriptor.setLastRun(rs.getTimestamp("last_run").toLocalDateTime());
                            }
                            if (rs.getString("params") != null) {
                                dsDescriptor.setParameters(mapper.readValue(rs.getString("params"), LinkedHashMap.class));
                            }
                            existingDataSources.add(dsDescriptor);
                        }
                    } catch (Exception e) {
                        logger.warning(String.format("Unable to retrieve %s - %s in data_sources_index table [%s]", dataSource.getId(), key.toString(), e.getMessage()));
                    }
                    boolean unfinised = existingDataSources.stream().filter(e -> e.getParameters() == null || e.getParameters().entrySet().stream().filter(p -> p.getValue() != null).count() > 0).count() > 0;
                    if (existingDataSources.isEmpty() || existingDataSources.get(0).getToYear() >= today.getYear() || unfinised) {
                        LinkedHashMap<String, String> parameters = (!existingDataSources.isEmpty() && existingDataSources.get(0).getParameters() != null) ? existingDataSources.get(0).getParameters() : new LinkedHashMap<>();
                        JSONObject parametersJson = new JSONObject();
                        parametersJson.putAll(parameters);
                        Map<String, String> coveragePeriod = retrieveTemporalCoverageBounds(((CollectionDescription) value).getTemporalCoverage());
                        if (coveragePeriod != null) {
                            int fromYear = Integer.parseInt(coveragePeriod.get("from"));
                            int toYear = coveragePeriod.get("to") != null ? Integer.parseInt(coveragePeriod.get("to")) : today.getYear();
                            LocalDateTime beginDate;
                            LocalDateTime endDate = LocalDateTime.of(toYear, 12, 31, 23, 59, 59);
                            logger.info(String.format("Starting %s - %s", dataSource.getId(), key));

                            DataSource<?, ?> instance = dataSourceManager.createInstance(key.toString(), dataSource.getId());
                            IndexedDataSourceParameter dataSourceDescr = readParamDescriptors.get(dataSource.getId());

                            // set data source credentials
                            Map<String, String> auth = dataSourceDescr.getAuthentication();
                            instance.setCredentials(auth.get("user"), auth.get("password"));

                            // insert data source into table or update last run field and get the id
                            try (Connection sqlConnection = source.getConnection();
                                 PreparedStatement stmtDs = sqlConnection.prepareStatement(insertDsStatement, Statement.RETURN_GENERATED_KEYS)) {
                                stmtDs.setString(1, dataSource.getId());
                                stmtDs.setString(2, key.toString());
                                stmtDs.setInt(3, fromYear);
                                stmtDs.setInt(4, toYear);
                                stmtDs.setObject(5, !existingDataSources.isEmpty() ? today : null);
                                stmtDs.execute();
                                ResultSet generatedKeys = stmtDs.getGeneratedKeys();
                                if (generatedKeys.next()) {
                                    dsKey.set(generatedKeys.getInt(1));
                                }
                            } catch (SQLException e) {
                                logger.warning(String.format("Creating indexed data source failed, no ID obtained [%s]", e.getMessage()));
                            }
                            // if the id data source is returned
                            if (dsKey.get() > 0) {
                                Map<String, DataSourceParameter> supportedParams = instance.getSupportedParameters().get(key);
                                final DataQuery query = instance.createQuery(key.toString());
                                List<DataQuery> queryList;
                                try {
                                    queryList = addParamsToQuery(dataSourceDescr, key.toString(), query);
                                    boolean skipBatch;
                                    if (query.supportsPaging()) {
                                        List<EOProduct> page;
                                        for (DataQuery q : queryList) {
                                            beginDate = LocalDateTime.of(fromYear, 1, 1, 0, 0, 0);
                                            //used to determine what parameter will be iterated (is set in the JSON file)
                                            String paramKey = q.getParameter(dataSourceDescr.getParamsPrefix()) != null ? q.getParameter(dataSourceDescr.getParamsPrefix()).getValue().toString() : q.getSensorName();
                                            int pageNumber = 1;

                                            try {
                                                q.setPageSize(PAGE_SIZE);
                                                q.setMaxResults(MAX_RECORDS);
                                                if (!existingDataSources.isEmpty() && existingDataSources.get(0).getParameters() != null) {
                                                    if (existingDataSources.get(0).getParameters().get(paramKey) != null) {
                                                        pageNumber = Integer.parseInt(existingDataSources.get(0).getParameters().get(paramKey));
                                                    } else if (existingDataSources.get(0).getParameters().containsKey(paramKey)) {
                                                        beginDate = existingDataSources.get(0).getLastRun();
                                                    }
                                                }
                                                setQueryPeriod(q, supportedParams, beginDate, endDate);
                                                logger.info(String.format("%s time period %s - %s", paramKey, beginDate, endDate));
                                                q.setPageNumber(pageNumber);
                                                q.setQueryDelay(5000);
                                                while (!(page = q.execute()).isEmpty()) {
                                                    try (Connection sqlConnection = source.getConnection();
                                                         PreparedStatement stmtDsProps = sqlConnection.prepareStatement(insertIndexedDsStatement);
                                                         PreparedStatement stmtDsUpdate = sqlConnection.prepareStatement(updateDsParamsStatement)) {
                                                        sqlConnection.setAutoCommit(false);
                                                        //set data source id
                                                        stmtDsProps.setInt(1, dsKey.get());

                                                        //set last run, data source name and sensor name
                                                        stmtDsUpdate.setObject(2, today);
                                                        stmtDsUpdate.setString(3, dataSource.getId());
                                                        stmtDsUpdate.setString(4, key.toString());
                                                        // add found products to batch
                                                        skipBatch = addProductsBatch(stmtDsProps, page, fromYear, toYear);
                                                        //execute batch if is not empty
                                                        if (!skipBatch) {
                                                            stmtDsProps.executeBatch();
                                                        }
                                                        //update parameters
                                                        parameters.put(paramKey, String.valueOf(pageNumber));
                                                        parametersJson.putAll(parameters);
                                                        stmtDsUpdate.setObject(1, parametersJson.toJSONString());
                                                        stmtDsUpdate.execute();

                                                        //execute transaction
                                                        sqlConnection.commit();

                                                        // good practice to set it back to default true
                                                        sqlConnection.setAutoCommit(true);
                                                        logger.info(String.format("%s Page %s completed", paramKey, pageNumber));
                                                        pageNumber++;
                                                        q.setPageNumber(pageNumber);
                                                    } catch (Exception e) {
                                                        logger.warning(String.format("Unable to index batch for %s page %s [%s]", q.getSensorName(), pageNumber, e.getMessage()));
                                                    }
                                                }
                                                try (Connection sqlConnection = source.getConnection();
                                                     PreparedStatement stmtDsUpdate = sqlConnection.prepareStatement(updateDsParamsStatement)) {
                                                    parameters.put(paramKey, null);
                                                    parametersJson.putAll(parameters);
                                                    stmtDsUpdate.setObject(1, parametersJson.toJSONString());
                                                    stmtDsUpdate.setObject(2, today);
                                                    //set data source name and sensor name
                                                    stmtDsUpdate.setString(3, dataSource.getId());
                                                    stmtDsUpdate.setString(4, key.toString());
                                                    stmtDsUpdate.execute();
                                                    q.setPageNumber(1);
                                                } catch (Exception e) {
                                                    logger.warning(String.format("Unable to update data source parameters for %s [%s]", q.getSensorName(), e.getMessage()));
                                                }
                                            } catch (Exception e) {
                                                logger.severe(String.format("Error for %s - %s page %s [%s]", q.getSensorName(), paramKey, pageNumber, e.getMessage()));
                                                try (Connection sqlConnection = source.getConnection();
                                                     PreparedStatement stmtDsUpdate = sqlConnection.prepareStatement(updateDsParamsStatement)) {
                                                    parameters.put(paramKey, String.valueOf(pageNumber));
                                                    parametersJson.putAll(parameters);
                                                    stmtDsUpdate.setObject(1, parametersJson.toJSONString());
                                                    stmtDsUpdate.setObject(2, today);
                                                    stmtDsUpdate.setString(3, dataSource.getId());
                                                    stmtDsUpdate.setString(4, key.toString());

                                                    stmtDsUpdate.execute();
                                                } catch (Exception ex) {
                                                    logger.warning(String.format("Unable to update database for %s [%s]", q.getSensorName(), e.getMessage()));
                                                }
                                            }
                                        }
                                    } else {
                                        for (DataQuery q : queryList) {
                                            //used to determine what parameter will be iterated (is set in the JSON file)
                                            String paramKey = q.getParameter(dataSourceDescr.getParamsPrefix()) != null ? q.getParameter(dataSourceDescr.getParamsPrefix()).getValue().toString() : q.getSensorName();
                                            beginDate = LocalDateTime.of(fromYear, 1, 1, 0, 0, 0);
                                            if (!existingDataSources.isEmpty() && existingDataSources.get(0).getParameters() != null) {
                                                if (existingDataSources.get(0).getParameters().containsKey(paramKey)) {
                                                    beginDate = existingDataSources.get(0).getLastRun();
                                                }
                                            }
                                            setQueryPeriod(q, supportedParams, beginDate, endDate);
                                            logger.info(String.format("%s time period %s - %s", paramKey, beginDate, endDate));
                                            q.setMaxResults(2000);
                                            List<EOProduct> productList = q.execute();
                                            if (!productList.isEmpty()) {
                                                try (Connection sqlConnection = source.getConnection();
                                                     PreparedStatement stmtDsProps = sqlConnection.prepareStatement(insertIndexedDsStatement);
                                                     PreparedStatement stmtDsUpdate = sqlConnection.prepareStatement(updateDsParamsStatement)) {
                                                    sqlConnection.setAutoCommit(false);
                                                    //set data source id
                                                    stmtDsProps.setInt(1, dsKey.get());
                                                    // add found products to batch
                                                    skipBatch = addProductsBatch(stmtDsProps, productList, fromYear, toYear);
                                                    //execute batch
                                                    if (!skipBatch) {
                                                        stmtDsProps.executeBatch();
                                                    }
                                                    parameters.put(paramKey, null);
                                                    parametersJson.putAll(parameters);
                                                    stmtDsUpdate.setObject(1, parametersJson.toJSONString());
                                                    stmtDsUpdate.setObject(2, today);
                                                    stmtDsUpdate.setString(3, dataSource.getId());
                                                    stmtDsUpdate.setString(4, key.toString());

                                                    stmtDsUpdate.execute();
                                                    //execute transaction
                                                    sqlConnection.commit();
                                                    // good practice to set it back to default true
                                                    sqlConnection.setAutoCommit(true);
                                                } catch (Exception ex) {
                                                    logger.warning(String.format("Unable to update database for %s [%s]", q.getSensorName(), ex.getMessage()));
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.warning(String.format("Invalid filter [%s]", e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * add parameters described in the JSON file to query
     */
    private List<DataQuery> addParamsToQuery(IndexedDataSourceParameter indexedDataSourceParameter, String sensor, DataQuery query) throws CloneNotSupportedException {
        List<DataQuery> queryList = new ArrayList<>();
        Map<String, Object> parameters = indexedDataSourceParameter.getSensor(sensor);
        if (parameters != null) {
            Map<String, ArrayList<String>> parametersToIterate = new HashMap<>();
            for (Iterator<Map.Entry<String, Object>> it = parameters.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Object> params = it.next();
                String key = params.getKey();
                Object value = params.getValue();
                if (value instanceof ArrayList) {
                    parametersToIterate.put(key, (ArrayList<String>) value);
                } else {
                    if (key.equalsIgnoreCase(CommonParameterNames.FOOTPRINT)) {
                        query.addParameter(key, Polygon2D.fromWKT((String) value));
                    } else {
                        query.addParameter(key, value);
                    }
                }
            }
            if (!parametersToIterate.isEmpty()) {
                for (Map.Entry<String, ArrayList<String>> entity : parametersToIterate.entrySet()) {
                    for (String entityValue : entity.getValue()) {
                        DataQuery tempQuery = query.clone();
                        tempQuery.addParameter(entity.getKey(), entityValue);
                        queryList.add(tempQuery);
                    }
                }
            } else {
                queryList.add(query);
            }
        } else {
            queryList.add(query);
        }
        return queryList;
    }

    /**
     * set query period based on start/end date type
     */
    private void setQueryPeriod(DataQuery q, Map<String, DataSourceParameter> supportedParams, LocalDateTime beginDate, LocalDateTime endDate) {
        if (supportedParams.containsKey(CommonParameterNames.START_DATE)) {
            if (supportedParams.get(CommonParameterNames.START_DATE).getType().isArray()) {
                QueryParameter begin = q.createParameter(CommonParameterNames.START_DATE,
                        LocalDateTime[].class,
                        new LocalDateTime[]{beginDate, endDate});
                begin.setMinValue(beginDate);
                begin.setMaxValue(endDate);
                q.addParameter(begin);
            } else {
                QueryParameter begin = q.createParameter(CommonParameterNames.START_DATE,
                        LocalDateTime.class,
                        beginDate);
                q.addParameter(begin);
                if (supportedParams.containsKey(CommonParameterNames.END_DATE)) {
                    QueryParameter<LocalDateTime> end = q.createParameter(CommonParameterNames.END_DATE, LocalDateTime.class, endDate);
                    q.addParameter(end);
                }
            }
        }
    }

    private void readPropFiles() {
        try {
            readParamDescriptors = JsonMapper.instance().readValue(readDescriptor(INDEXING_PARAMETER_DESCRIPTOR_RESOURCE),
                    JSON_INDEXED_DATA_SOURCE_REFERENCE);
        } catch (Exception e) {
            logger.severe(String.format("Cannot load data source supported parameters. Cause: %s", e.getMessage()));
        }
    }

    private String readDescriptor(String fileName) throws IOException, URISyntaxException {
        final String classLocation = getClass().getClassLoader().getResource(fileName).toURI().toString();
        Path rPath;
        rPath = Paths.get(URI.create(classLocation));
        return new String(Files.readAllBytes(rPath));
    }

    private boolean addProductsBatch(PreparedStatement stmt, List<EOProduct> productList, int fromYear, int toYear) {
        int prodInBatch = 0;
        for (EOProduct prod : productList) {
            try {
                LocalDateTime acquisitionDate = prod.getAcquisitionDate();
                String footprint = prod.getGeometry();
                //when acquisition date is not set, try to retrieve it from product name
                if (acquisitionDate == null) {
                    Matcher matcher = acquisitionDatePattern.matcher(prod.getName());
                    if (matcher.find()) {
                        //check if the found number sequence is a valid date YYYYMMDD
                        if (Pattern.compile("(\\d{4})(0[1-9]|1[0-2])(0[1-9]|[1-2]\\d|3[0-1])").matcher(matcher.group(2)).find()) {
                            LocalDate foundDate = DateUtils.parseDate(matcher.group(2));
                            // fromYear and toYear are used to ensure that the extracted year is correct
                            if (foundDate.getYear() >= fromYear && foundDate.getYear() <= toYear) {
                                acquisitionDate = LocalDateTime.of(foundDate.getYear(), foundDate.getMonth(), foundDate.getDayOfMonth(), 0, 0, 0);
                            }
                        }
                    }
                }
                //when footprint is not set, set it to whole globe
                if (footprint == null) {
                    footprint = "POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))";
                } else {
                    Polygon2D polygon2D = Polygon2D.fromWKT(footprint);
                    // simplify polygon if has has more than 100 points
                    if (polygon2D != null && polygon2D.getNumPoints() > 0) {
                        if (polygon2D.getNumPoints() < 100) {
                            footprint = polygon2D.toWKT();
                        } else {
                            Geometry siplyfiedPoligon = DouglasPeuckerSimplifier.simplify(new GeometryAdapter().marshal(footprint), 1);
                            footprint = new GeometryAdapter().unmarshal(siplyfiedPoligon);
                        }
                    }
                }
                stmt.setObject(3, acquisitionDate);
                stmt.setString(2, footprint);
                stmt.addBatch();
                prodInBatch++;
            } catch (Exception e) {
                logger.info(String.format("Product [%s] was not added to batch. [%s]", prod.getName(), e.getMessage()));
            }
        }
        return prodInBatch == 0;
    }

    private Map<String, String> retrieveTemporalCoverageBounds(String temporalCoverage) {
        Matcher matcher = temporalPattern.matcher(temporalCoverage);
        boolean matchFound = matcher.find();
        if (matchFound) {
            return new HashMap() {
                {
                    put("from", matcher.group(1));
                    put("to", matcher.group(3) != null && !matcher.group(3).equalsIgnoreCase("present") ? matcher.group(3) : null);
                }
            };
        }
        return null;
    }
}
