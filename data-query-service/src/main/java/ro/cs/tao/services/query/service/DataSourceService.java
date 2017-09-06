package ro.cs.tao.services.query.service;

import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.services.query.model.Query;

import java.text.ParseException;
import java.util.List;

/**
 * @author Cosmin Cara
 */
public interface DataSourceService {

    List<String> getSupportedSensors();

    List<String> getDatasourcesForSensor(String sensorName);

    List<ParameterDescriptor> getSupportedParameters(String sensorName, String dataSourceClassName);

    List<EOProduct> query(String sensorName, String dataSourceClass, Query queryObject) throws ParseException;
}
