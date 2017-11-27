package ro.cs.tao.services.query.service;

import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.query.model.DataSourceInstance;
import ro.cs.tao.services.query.model.Query;

import java.util.List;
import java.util.SortedSet;

/**
 * @author Cosmin Cara
 */
public interface DataSourceService {

    SortedSet<String> getSupportedSensors();

    List<String> getDatasourcesForSensor(String sensorName);

    List<DataSourceInstance> getDatasourceInstances();

    List<ParameterDescriptor> getSupportedParameters(String sensorName, String dataSourceName);

    List<EOProduct> query(Query queryObject) throws SerializationException;

    List<EOProduct> fetch(Query queryObject, List<EOProduct> products);

}
