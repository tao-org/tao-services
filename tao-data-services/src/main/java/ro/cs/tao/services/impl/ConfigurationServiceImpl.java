package ro.cs.tao.services.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("configurationService")
public class ConfigurationServiceImpl
    extends EntityService<KeyValuePair>
        implements ConfigurationService {

    @Override
    public KeyValuePair findById(String id) {
        final String value = ConfigurationManager.getInstance().getValue(id);
        return value != null ? new KeyValuePair(id, value) : null;
    }

    @Override
    public List<KeyValuePair> list() {
        final Map<String, String> properties = ConfigurationManager.getInstance().getAll();
        List<KeyValuePair> result = null;
        if (properties != null) {
            result = properties.entrySet().stream()
                    .map(e -> new KeyValuePair(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
        return result;
    }

    @Override
    public void save(KeyValuePair object) {
        //TODO: delegate to user preferences
    }

    @Override
    public void update(KeyValuePair object) {
        //TODO: delegate to user preferences
    }

    @Override
    public void delete(String id) {
        //TODO: delegate to user preferences
    }

    @Override
    protected void validateFields(KeyValuePair object, List<String> errors) {
        String value = object.getKey();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[key] cannot be empty");
        }
        value = object.getValue();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[value] cannot be empty");
        }
    }
}
