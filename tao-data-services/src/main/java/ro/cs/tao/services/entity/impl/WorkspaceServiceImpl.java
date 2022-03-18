package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.WorkspaceProvider;
import ro.cs.tao.services.interfaces.WorkspaceService;
import ro.cs.tao.topology.Workspace;
import ro.cs.tao.topology.WorkspaceType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for Workspace entity service.
 *
 * @author Cosmin Cara
 */
@Service("workspaceService")
public class WorkspaceServiceImpl
        extends EntityService<Workspace> implements WorkspaceService {
    @Autowired
    private WorkspaceProvider workspaceProvider;

    @Override
    public Workspace findById(Integer id) {
        return workspaceProvider.get(id);
    }

    @Override
    public List<Workspace> list() {
        return workspaceProvider.list();
    }

    @Override
    public List<Workspace> getByUser(String userName) {
        return workspaceProvider.getByUser(userName);
    }

    @Override
    public List<Workspace> list(Iterable<Integer> ids) {
        return workspaceProvider.list(ids);
    }

    @Override
    public Workspace save(Workspace object) {
        try {
            return workspaceProvider.save(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public Workspace update(Workspace object) throws PersistenceException {
        Workspace existing = workspaceProvider.get(object.getId());
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
    protected void validateFields(Workspace entity, List<String> errors) {
        String value = entity.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[name] cannot be empty");
        }
        value = entity.getDescription();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[description] cannot be empty");
        }
        final WorkspaceType type = entity.getType();
        final List<String> paramNames = type.getParameters();
        final Map<String, String> parameters = entity.getParameters();
        for (String pName : paramNames) {
            if (parameters.get(pName) == null) {
                errors.add("[parameters] " + pName + " not set");
            }
        }
    }
}
