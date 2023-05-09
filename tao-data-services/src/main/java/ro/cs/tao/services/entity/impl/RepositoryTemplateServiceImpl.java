package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryTemplateProvider;
import ro.cs.tao.services.interfaces.RepositoryTemplateService;
import ro.cs.tao.workspaces.RepositoryTemplate;
import ro.cs.tao.workspaces.RepositoryType;

import java.util.*;

@Service("repositoryTemplateService")
public class RepositoryTemplateServiceImpl
        extends EntityService<RepositoryTemplate> implements RepositoryTemplateService {

    @Autowired
    private RepositoryTemplateProvider repositoryTemplateProvider;

    @Override
    public RepositoryTemplate findById(String id) {
        return repositoryTemplateProvider.get(id);
    }

    @Override
    public List<RepositoryTemplate> list() {
        List<RepositoryTemplate> list = repositoryTemplateProvider.list();
        list.sort(Comparator.comparing(RepositoryTemplate::getName));
        return list;
    }

    @Override
    public List<RepositoryTemplate> list(Iterable<String> ids) {
        return repositoryTemplateProvider.list();
    }

    @Override
    public RepositoryTemplate save(RepositoryTemplate object) {
        try {
            if (object.getId() == null) {
                object.setId(UUID.randomUUID().toString());
            }
            return repositoryTemplateProvider.save(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public RepositoryTemplate update(RepositoryTemplate object) throws PersistenceException {
        RepositoryTemplate existing = repositoryTemplateProvider.get(object.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Entity does not exist. Call save() instead");
        }
        existing.setName(object.getName());
        existing.setDescription(object.getDescription());
        existing.setType(object.getType());
        final LinkedHashMap<String, String> parameters = object.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            throw new IllegalArgumentException("Repository template parameters not supplied");
        }
        existing.setParameters(parameters);
        return repositoryTemplateProvider.update(existing);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        if (id != null) {
            repositoryTemplateProvider.delete(id);
        }
    }

    @Override
    protected void validateFields(RepositoryTemplate entity, List<String> errors) {
        String value = entity.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[name] cannot be empty");
        }
        value = entity.getDescription();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[description] cannot be empty");
        }
        final RepositoryType type = entity.getType();
        final Map<String, Boolean> paramNames = type.getParameters();
        final Map<String, String> parameters = entity.getParameters();
        for (Map.Entry<String, Boolean> entry : paramNames.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue()) && parameters.get(entry.getKey()) == null) {
                errors.add("[parameters] " + entry.getKey() + " not set");
            }
        }
    }
}
