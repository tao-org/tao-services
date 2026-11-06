package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.commons.CapabilityTokens;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
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

    private static final String tokenJSON = "{ \"repository\":\"%s\", \"fileName\":%s, \"folder\":%s, \"user\":\"%s\", \"base64\":\"%s\" }";

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
    public List<Repository> getByUser(String userId) {
        return workspaceProvider.getByUser(userId);
    }

    @Override
    public Repository getByUserAndName(String userId, String repositoryName) {
        return workspaceProvider.getByUserAndName(userId, repositoryName);
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
        requireOwner(existing);
        if (existing.isSystem()) {
            throw new org.springframework.security.access.AccessDeniedException("System repositories cannot be changed");
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
            Repository existing = workspaceProvider.get(id);
            requireOwner(existing);
            if (existing.isSystem()) {
                throw new org.springframework.security.access.AccessDeniedException("System repositories cannot be deleted");
            }
            workspaceProvider.delete(id);
        }
    }

    @Override
    public String createDownloadURL(String userId, String repositoryId, String item, boolean isFile, boolean base64Encoded) {
        if (StringUtilities.isNullOrEmpty(item)) {
            throw new IllegalArgumentException("Empty item");
        }
        final Repository repository = workspaceProvider.get(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("[repository] Invalid value");
        }
        List<Repository> userRepositories = workspaceProvider.getByUser(userId);
        if (userRepositories.stream().noneMatch(r -> repositoryId.equals(r.getId()))) {
            throw new IllegalArgumentException("Operation not allowed");
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("repository", repositoryId);
        values.put(isFile ? "fileName" : "folder", item);
        values.put("user", userId);
        values.put("base64", Boolean.toString(base64Encoded));
        final String json = new ObjectMapper().writeValueAsString(values);
        return "/files/get?token=" + URLEncoder.encode(
                CapabilityTokens.issue("download", json, Duration.ofMinutes(15)), StandardCharsets.UTF_8);
    }

    private void requireOwner(Repository repository) {
        java.security.Principal principal = SessionStore.currentContext().getPrincipal();
        String owner = principal instanceof UserPrincipal user && user.getMasterId() != null
                ? user.getMasterId() : principal.getName();
        if (repository == null || !(principal instanceof SystemPrincipal)
                && !owner.equals(repository.getUserId()) && !principal.getName().equals(repository.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Operation not allowed");
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
