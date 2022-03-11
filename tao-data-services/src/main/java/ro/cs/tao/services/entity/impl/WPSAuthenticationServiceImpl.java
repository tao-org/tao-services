package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.WPSAuthentication;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.WPSAuthenticationProvider;
import ro.cs.tao.services.interfaces.WPSAuthenticationService;

import java.util.List;
import java.util.logging.Logger;

@Service("wpsAuthenticationService")
public class WPSAuthenticationServiceImpl
        extends EntityService<WPSAuthentication> implements WPSAuthenticationService {

    @Autowired
    private WPSAuthenticationProvider wpsAuthenticationProvider;

    private Logger logger = Logger.getLogger(WPSAuthenticationService.class.getName());

    @Override
    public WPSAuthentication findById(String id) {
        return wpsAuthenticationProvider.get(id);
    }

    @Override
    public List<WPSAuthentication> list() {
        return wpsAuthenticationProvider.list();
    }

    @Override
    public List<WPSAuthentication> list(Iterable<String> ids) {
        return wpsAuthenticationProvider.list(ids);
    }

    @Override
    public WPSAuthentication save(WPSAuthentication object) {
        try {
            return wpsAuthenticationProvider.save(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public WPSAuthentication update(WPSAuthentication object) throws PersistenceException {
        return wpsAuthenticationProvider.update(object);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        wpsAuthenticationProvider.delete(id);
    }

    @Override
    protected void validateFields(WPSAuthentication entity, List<String> errors) {
        String value = entity.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        if (entity.getType() == null) {
            errors.add("[type] cannot be null");
        }
    }
}
