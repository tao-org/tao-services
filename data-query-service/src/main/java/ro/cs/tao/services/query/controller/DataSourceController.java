package ro.cs.tao.services.query.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.query.service.DataSourceService;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/query")
public class DataSourceController {

    @Autowired
    private DataSourceService dataSourceService;

    @RequestMapping(value = "/sensor/", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getSupportedSensors() {
        List<String> sensors = dataSourceService.getSupportedSensors();
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
}
