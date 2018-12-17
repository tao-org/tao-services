/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.GroupComponent;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.GroupComponentService;

import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Cosmin Cara
 */
@Service("groupComponentService")
public class GroupComponentServiceImpl
        extends EntityService<GroupComponent>
        implements GroupComponentService {

    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(ComponentService.class.getName());

    @Override
    public GroupComponent findById(String id) {
        GroupComponent component = null;
        component = persistenceManager.getGroupComponentById(id);
        return component;
    }

    @Override
    public List<GroupComponent> list() {
        List<GroupComponent> components = null;
        try {
            components = persistenceManager.getGroupComponents();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public List<GroupComponent> list(Iterable<String> ids) {
        List<GroupComponent> components = null;
        try {
            components = persistenceManager.getGroupComponents(ids);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public GroupComponent save(GroupComponent component) {
        try {
            return persistenceManager.saveGroupComponent(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public GroupComponent update(GroupComponent component) {
        try {
            return persistenceManager.updateGroupComponent(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) {
        if (id != null) {
            try {
                persistenceManager.deleteGroupComponent(id);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    @Override
    protected void validateFields(GroupComponent entity, List<String> errors) {

    }
}
