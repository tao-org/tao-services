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
import ro.cs.tao.persistence.GroupComponentProvider;
import ro.cs.tao.persistence.PersistenceException;
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
    private GroupComponentProvider componentProvider;
    private Logger logger = Logger.getLogger(ComponentService.class.getName());

    @Override
    public GroupComponent findById(String id) {
        return componentProvider.get(id);
    }

    @Override
    public List<GroupComponent> list() {
        return componentProvider.list();
    }

    @Override
    public List<GroupComponent> list(Iterable<String> ids) {
        return componentProvider.list(ids);
    }

    @Override
    public GroupComponent save(GroupComponent component) {
        try {
            return componentProvider.save(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public GroupComponent update(GroupComponent component) {
        try {
            return componentProvider.update(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) {
        if (id != null) {
            try {
                componentProvider.delete(id);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    @Override
    protected void validateFields(GroupComponent entity, List<String> errors) {

    }
}
