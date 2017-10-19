package ro.cs.tao.services.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.KeyValuePair;
import ro.cs.tao.services.interfaces.ConfigurationService;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * @author Cosmin Cara
 */
@Service("configurationService")
public class ConfigurationServiceImpl
    extends ServiceBase<KeyValuePair>
        implements ConfigurationService {

    @Override
    public KeyValuePair findById(String id) {
        final String value = ConfigurationManager.getInstance().getValue(id);
        return value != null ? new KeyValuePair(id, value) : null;
    }

    @Override
    public List<KeyValuePair> list() {
        final Properties properties = ConfigurationManager.getInstance().getAll();
        List<KeyValuePair> result = null;
        if (properties != null) {
            result = new ArrayList<>();
            final Enumeration<?> names = properties.propertyNames();
            while (names.hasMoreElements()) {
                final String name = (String) names.nextElement();
                result.add(new KeyValuePair(name, properties.getProperty(name)));
            }
        }
        return result;
    }

    @Override
    public void save(KeyValuePair object) {
        // no-op
    }

    @Override
    public void update(KeyValuePair object) {
        // no-op
    }

    @Override
    public void delete(String id) {
        // no-op
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
