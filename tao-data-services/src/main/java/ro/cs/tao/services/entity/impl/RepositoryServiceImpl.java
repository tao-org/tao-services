package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for Repository entity service.
 *
 * @author Cosmin Cara
 */
@Service("repositoryService")
public class RepositoryServiceImpl
        extends EntityService<Repository> implements RepositoryService {
    @Autowired
    private RepositoryProvider workspaceProvider;

    @Override
    public Repository findById(Integer id) {
        return workspaceProvider.get(id);
    }

    @Override
    public List<Repository> list() {
        return workspaceProvider.list();
    }

    @Override
    public List<Repository> getByUser(String userName) {
        return workspaceProvider.getByUser(userName);
    }

    @Override
    public List<Repository> list(Iterable<Integer> ids) {
        return workspaceProvider.list(ids);
    }

    @Override
    public Repository save(Repository object) {
        try {
            return workspaceProvider.save(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public Repository update(Repository object) throws PersistenceException {
        Repository existing = workspaceProvider.get(object.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Entity does not exist. Call save() instead");
        }
        existing.setName(object.getName());
        existing.setDescription(object.getDescription());
        existing.setType(object.getType());
        final LinkedHashMap<String, String> parameters = object.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            throw new IllegalArgumentException("Worspace parameters not supplied");
        }
        existing.setParameters(parameters);
        return workspaceProvider.update(existing);
    }

    @Override
    public void delete(Integer id) throws PersistenceException {
        if (id != null && id > 1) {
            workspaceProvider.delete(id);
        }
    }

    @Override
    protected void validateFields(Repository entity, List<String> errors) {
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
