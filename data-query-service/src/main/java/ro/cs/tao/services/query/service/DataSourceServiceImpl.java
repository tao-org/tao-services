package ro.cs.tao.services.query.service;

import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.param.ParameterDescriptor;

import java.util.ArrayList;
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
}
