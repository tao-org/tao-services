package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.SiteProvider;
import ro.cs.tao.services.interfaces.SiteService;
import ro.cs.tao.workspaces.Site;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation for Repository entity service.
 *
 * @author Cosmin Cara
 */
@Service("siteService")
public class SiteServiceImpl
        extends EntityService<Site> implements SiteService {

    @Autowired
    private SiteProvider siteProvider;

    @Override
    public Site findById(String id) {
        return this.siteProvider.get(id);
    }

    @Override
    public List<Site> list() {
        return this.siteProvider.list();
    }

    @Override
    public List<Site> list(Iterable<String> ids) {
        return this.siteProvider.list();
    }

    @Override
    public Site save(Site object) {
        try {
            if (object.getId() ==  null) {
                object.setId(UUID.randomUUID().toString());
            }
            return this.siteProvider.save(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public Site update(Site object) throws PersistenceException {
        Site existing = this.siteProvider.get(object.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Entity does not exist. Call save() instead");
        }
        existing.setName(object.getName());
        existing.setDescription(object.getDescription());
        existing.setFootprint(object.getFootprint());
        existing.setStartDate(object.getStartDate());
        existing.setEndDate(object.getEndDate());
        return this.siteProvider.update(existing);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        this.siteProvider.delete(id);
    }

    @Override
    public List<Site> getByUser(String userName) {
        return this.siteProvider.getByUser(userName);
    }

    @Override
    protected void validateFields(Site entity, List<String> errors) {
        String value = entity.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[name] cannot be empty");
        }
        value = entity.getDescription();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[description] cannot be empty");
        }
        if (entity.getFootprint() == null) {
            errors.add("[footprint] cannot be empty");
        }
        LocalDateTime date = entity.getStartDate();
        if (date == null) {
            errors.add("[startDate] cannot be empty");
        }
        date = entity.getEndDate();
        if (date == null) {
            errors.add("[endDate] cannot be empty");
        }
        if (date != null && entity.getStartDate() != null && date.isBefore(entity.getStartDate())) {
            errors.add("[endDate] cannot be before [startDate]");
        }
    }
}
