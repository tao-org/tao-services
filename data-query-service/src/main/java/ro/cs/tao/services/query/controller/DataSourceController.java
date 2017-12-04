package ro.cs.tao.services.query.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.model.datasource.DataSourceInstance;
import ro.cs.tao.services.model.datasource.Query;

import java.util.List;
import java.util.SortedSet;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/query")
public class DataSourceController extends BaseController {

    @Autowired
    private DataSourceService dataSourceService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<DataSourceInstance>> getRegisteredSources() {
        List<DataSourceInstance> instances = dataSourceService.getDatasourceInstances();
        if (instances == null || instances.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(instances, HttpStatus.OK);
    }

    @RequestMapping(value = "/sensor/", method = RequestMethod.GET)
    public ResponseEntity<SortedSet<String>> getSupportedSensors() {
        SortedSet<String> sensors = dataSourceService.getSupportedSensors();
        if (sensors == null || sensors.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(sensors, HttpStatus.OK);
    }

    @RequestMapping(value = "/sensor/{name}", method = RequestMethod.GET)
    public ResponseEntity<?> getDatasourcesForSensor(@PathVariable("name") String sensorName) {
        List<String> sources = dataSourceService.getDatasourcesForSensor(sensorName);
        if (sources == null) {
            return new ResponseEntity<>(new ServiceError(String.format("No data source available for [%s]", sensorName)),
                                        HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(sources, HttpStatus.OK);
    }

    @RequestMapping(value = "/sensor/{name}/{source:.+}", method = RequestMethod.GET)
    public ResponseEntity<?> getSupportedParameters(@PathVariable("name") String sensorName,
                                                                            @PathVariable("source") String dataSourceClassName) {
        List<ParameterDescriptor> params = dataSourceService.getSupportedParameters(sensorName, dataSourceClassName);
        if (params == null) {
            return new ResponseEntity<>(new ServiceError(String.format("No data source available for [%s]", sensorName)),
                                        HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(params, HttpStatus.OK);
    }

    @RequestMapping(value = "/exec", method = RequestMethod.POST)
    public ResponseEntity<?> doQuery(@RequestBody Query query) {
        List<ParameterDescriptor> params = dataSourceService.getSupportedParameters(query.getSensor(),
                                                                                    query.getDataSource());
        if (params == null) {
            return new ResponseEntity<>(new ServiceError(String.format("No data source named [%s] available for [%s]",
                                                                       query.getDataSource(),
                                                                       query.getSensor())),
                                        HttpStatus.BAD_REQUEST);
        }
        try {
            final List<EOProduct> results = dataSourceService.query(query);
            if (results == null) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (SerializationException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
