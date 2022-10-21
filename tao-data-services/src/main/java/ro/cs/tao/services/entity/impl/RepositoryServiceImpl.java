package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public Repository findById(String id) {
        return workspaceProvider.get(id);
    }

    @Override
    public List<Repository> list() {
        final List<Repository> list = workspaceProvider.list();
        list.sort((o1, o2) -> o1.getCreated() == null ? -1 : o1.getCreated().compareTo(o2.getCreated()));
        return list;
    }

    @Override
    public List<Repository> getByUser(String userName) {
        return workspaceProvider.getByUser(userName);
    }

    @Override
    public Repository getByUserAndName(String userName, String repositoryName) {
        return workspaceProvider.getByUserAndName(userName, repositoryName);
    }

    @Override
    public List<Repository> list(Iterable<String> ids) {
        return workspaceProvider.list(ids);
    }

    @Override
    public Repository save(Repository object) {
        try {
            if (object.getId() == null) {
                object.setId(UUID.randomUUID().toString());
            }
            Repository repository = workspaceProvider.save(object);
            try {
                StorageService service = StorageServiceFactory.getInstance(repository);
                Path path = null;
                if (service != null) {
                    path = service.createFolder("test_write", true);
                    if (path != null) {
                        service.remove("test_write");
                    }
                }
                repository.setReadOnly(path == null);
            } catch (Exception e) {
                repository.setReadOnly(true);
            }
            return workspaceProvider.update(repository);
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
    public void delete(String id) throws PersistenceException {
        if (id != null) {
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
