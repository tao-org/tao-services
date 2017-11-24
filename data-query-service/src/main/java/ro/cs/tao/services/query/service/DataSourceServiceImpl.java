package ro.cs.tao.services.query.service;

import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.services.query.model.DataSourceInstance;
import ro.cs.tao.services.query.model.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author Cosmin Cara
 */
@Service("dataSourceService")
public class DataSourceServiceImpl implements DataSourceService {

    @Override
    public SortedSet<String> getSupportedSensors() {
        return DataSourceManager.getInstance().getSupportedSensors();
    }

    @Override
    public List<String> getDatasourcesForSensor(String sensorName) {
        return DataSourceManager.getInstance().getNames(sensorName);
    }

    @Override
    public List<DataSourceInstance> getDatasourceInstances() {
        List<DataSourceInstance> instances = new ArrayList<>();
        final DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        final SortedSet<String> sensors = dataSourceManager.getSupportedSensors();
        for (String sensor : sensors) {
            dataSourceManager.getNames(sensor).forEach(n ->
                instances.add(new DataSourceInstance(sensor, n, dataSourceManager.getSupportedParameters(sensor, n))));
        }
        return instances;
    }

    @Override
    public List<ParameterDescriptor> getSupportedParameters(String sensorName, String dataSourceName) {
        Map<String, ParameterDescriptor> parameterDescriptorMap =
                DataSourceManager.getInstance().getSupportedParameters(sensorName, dataSourceName);
        List<ParameterDescriptor> parameters = null;
        if (parameterDescriptorMap != null) {
            parameters = new ArrayList<>(parameterDescriptorMap.values());
        }
        return parameters;
    }

    @Override
    public List<EOProduct> query(String sensorName, String dataSourceName, Query queryObject) throws ParseException {
        List<EOProduct> results = null;
        if (queryObject != null) {
            DataSourceComponent dsComponent = new DataSourceComponent(sensorName, dataSourceName);
            dsComponent.setUserCredentials(queryObject.getUser(), queryObject.getPassword());
            final Map<String, ParameterDescriptor> parameterDescriptorMap =
                    DataSourceManager.getInstance().getSupportedParameters(sensorName, dataSourceName);
            DataQuery query = dsComponent.createQuery();
            Map<String, Object> paramValues = queryObject.getValues();
            for (Map.Entry<String, Object> entry : paramValues.entrySet()) {
                final ParameterDescriptor descriptor = parameterDescriptorMap.get(entry.getKey());
                final Class type = descriptor.getType();
                final QueryParameter queryParameter =
                        query.createParameter(entry.getKey(),
                                              type,
                                              Date.class.isAssignableFrom(type) ?
                                                      new SimpleDateFormat("yyyy-MM-dd").parse(String.valueOf(entry.getValue()))
                                                      : entry.getValue());
                query.addParameter(queryParameter);
            }
            results = query.execute();
        }
        return results;
    }

    @Override
    public List<EOProduct> fetch(String sensorName, String dataSourceName, List<EOProduct> products, String user, String password) {
        if (products != null) {
            DataSourceComponent dsComponent = new DataSourceComponent(sensorName, dataSourceName);
            dsComponent.setUserCredentials(user, password);
            String path = ConfigurationManager.getInstance().getValue("product.location");
            products = dsComponent.doFetch(products, path);
        }
        return products;
    }
}
