package ro.cs.tao.services.query.service;

import ro.cs.tao.datasource.param.ParameterDescriptor;

import java.util.List;

/**
 * @author Cosmin Cara
 */
public interface DataSourceService {

    List<String> getSupportedSensors();

    List<String> getDatasourcesForSensor(String sensorName);

    List<ParameterDescriptor> getSupportedParameters(String sensorName, String dataSourceClassName);

}
