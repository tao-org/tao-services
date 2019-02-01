package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.Tag;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.DataSourceGroupService;

import java.util.List;

@Service("dataSourceGroupService")
public class DataSourceGroupServiceImpl implements DataSourceGroupService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public DataSourceComponentGroup findById(String id) throws PersistenceException {
        return persistenceManager.getDataSourceComponentGroup(id);
    }

    @Override
    public List<DataSourceComponentGroup> list() {
        return persistenceManager.getDataSourceComponentGroups();
    }

    @Override
    public List<DataSourceComponentGroup> list(Iterable<String> ids) {
        return persistenceManager.getDataSourceComponentGroups(ids);
    }

    @Override
    public DataSourceComponentGroup save(DataSourceComponentGroup object) {
        try {
            return persistenceManager.saveDataSourceComponentGroup(object);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public DataSourceComponentGroup update(DataSourceComponentGroup object) throws PersistenceException {
        return persistenceManager.updateDataSourceComponentGroup(object);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        persistenceManager.deleteDataSourceComponentGroup(id);
    }

    @Override
    public List<DataSourceComponentGroup> getUserDataSourceComponentGroups(String userName) {
        return persistenceManager.getUserDataSourceComponentGroup(userName);
    }

    @Override
    public List<DataSourceComponentGroup> getDataSourceComponentGroups() {
        return persistenceManager.getDataSourceComponentGroups();
    }

    @Override
    public List<Tag> getDatasourceGroupTags() {
        return persistenceManager.getDatasourceTags();
    }
}
