package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.WebServiceAuthentication;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.WebServiceAuthenticationProvider;
import ro.cs.tao.services.interfaces.WebServiceAuthenticationService;

import java.util.List;
import java.util.logging.Logger;

@Service("webServiceAuthenticationService")
public class WebServiceAuthenticationServiceImpl
        extends EntityService<WebServiceAuthentication> implements WebServiceAuthenticationService {

    @Autowired
    private WebServiceAuthenticationProvider webServiceAuthenticationProvider;

    private Logger logger = Logger.getLogger(WebServiceAuthenticationService.class.getName());

    @Override
    public WebServiceAuthentication findById(String id) {
        return webServiceAuthenticationProvider.get(id);
    }

    @Override
    public List<WebServiceAuthentication> list() {
        return webServiceAuthenticationProvider.list();
    }

    @Override
    public List<WebServiceAuthentication> list(Iterable<String> ids) {
        return webServiceAuthenticationProvider.list(ids);
    }

    @Override
    public WebServiceAuthentication save(WebServiceAuthentication object) {
        try {
            return webServiceAuthenticationProvider.save(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public WebServiceAuthentication update(WebServiceAuthentication object) throws PersistenceException {
        return webServiceAuthenticationProvider.update(object);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        webServiceAuthenticationProvider.delete(id);
    }

    @Override
    protected void validateFields(WebServiceAuthentication entity, List<String> errors) {
        String value = entity.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        if (entity.getType() == null) {
            errors.add("[type] cannot be null");
        }
    }
}
