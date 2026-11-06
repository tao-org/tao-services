package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.gadm.AdminL1Unit;
import ro.cs.tao.gadm.AdminL2Unit;
import ro.cs.tao.gadm.Country;
import ro.cs.tao.persistence.*;
import ro.cs.tao.services.interfaces.SiteService;
import ro.cs.tao.workspaces.Site;

import java.time.LocalDateTime;
import java.util.Comparator;
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

    @Autowired
    private CountryProvider countryProvider;

    @Autowired
    private AdminL1UnitProvider adminL1UnitProvider;

    @Autowired
    private AdminL2UnitProvider adminL2UnitProvider;

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
        existing.setAdministrativeArea(object.getAdministrativeArea());
        existing.setStartDate(object.getStartDate());
        existing.setEndDate(object.getEndDate());
        return this.siteProvider.update(existing);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        this.siteProvider.delete(id);
    }

    @Override
    public List<Site> getByUser(String userId) {
        return this.siteProvider.getByUser(userId);
    }

    @Override
    public List<Country> getCountries() {
        final List<Country> countries = this.countryProvider.list();
        countries.sort(new Comparator<Country>() {
            @Override
            public int compare(Country c1, Country c2) {
                return c1.getName().compareTo(c2.getName());
            }
        });
        return countries;
    }

    @Override
    public Country getCountryByName(String name) {
        return this.countryProvider.getByName(name);
    }

    @Override
    public Country getCountryByCode(String isoCode) {
        return this.countryProvider.getByIso3Code(isoCode);
    }

    @Override
    public void saveCountries(List<Country> countries) {
        this.countryProvider.saveAll(countries);
    }

    @Override
    public List<AdminL1Unit> getAdminL1Units(String countryCode) {
        return this.adminL1UnitProvider.getByCountryCode(countryCode);
    }

    @Override
    public AdminL1Unit getAdminL1UnitByName(String name) {
        return this.adminL1UnitProvider.getByName(name);
    }

    @Override
    public void updateCountry(String countryCode, List<AdminL1Unit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        units.forEach(unit -> { unit.setCountryCode(countryCode);});
        this.adminL1UnitProvider.saveAll(units);
    }

    @Override
    public List<AdminL2Unit> getAdminL2Units(String adminL1Id) {
        return this.adminL2UnitProvider.getByAdminL1Id(adminL1Id);
    }

    @Override
    public AdminL2Unit getAdminL2UnitByName(String name) {
        return this.adminL2UnitProvider.getByName(name);
    }

    @Override
    public void updateAdminL1(String adminL1Id, List<AdminL2Unit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        units.forEach(unit -> { unit.setAdminL1Id(adminL1Id);});
        this.adminL2UnitProvider.saveAll(units);
    }

    @Override
    public void saveAdminL2Units(List<AdminL2Unit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        this.adminL2UnitProvider.saveAll(units);
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
