package ro.cs.tao.services.query.service;

import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.services.query.model.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Cara
 */
@Service("dataSourceService")
public class DataSourceServiceImpl implements DataSourceService {

    @Override
    public List<String> getSupportedSensors() {
        return DataSourceManager.getInstance().getSupportedSensors();
    }

    @Override
    public List<String> getDatasourcesForSensor(String sensorName) {
        return DataSourceManager.getInstance().getNames(sensorName);
    }

    @Override
    public List<ParameterDescriptor> getSupportedParameters(String sensorName, String dataSourceClassName) {
        Map<String, ParameterDescriptor> parameterDescriptorMap =
                DataSourceManager.getInstance().getSupportedParameters(sensorName, dataSourceClassName);
        List<ParameterDescriptor> parameters = null;
        if (parameterDescriptorMap != null) {
            parameters = new ArrayList<>(parameterDescriptorMap.values());
        }
        return parameters;
    }

    @Override
    public List<EOProduct> query(String sensorName, String dataSourceClass, Query queryObject) throws ParseException {
        List<EOProduct> results = null;
        if (queryObject != null) {
            DataSource dataSource = DataSourceManager.getInstance().get(sensorName,
                                                                        dataSourceClass);
            if (dataSource != null) {
                dataSource.setCredentials(queryObject.getUser(), queryObject.getPassword());
                final Map<String, ParameterDescriptor> parameterDescriptorMap =
                        DataSourceManager.getInstance().getSupportedParameters(sensorName, dataSourceClass);
                DataQuery query = dataSource.createQuery(sensorName);
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
        }
        return results;
    }
}
