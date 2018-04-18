/*
 * Copyright (C) 2017 CS ROMANIA
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

import org.springframework.stereotype.Service;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.docker.Container;
import ro.cs.tao.services.interfaces.TopologyService;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Service("topologyService")
public class TopologyServiceImpl
    extends EntityService<NodeDescription>
        implements TopologyService {

    @Override
    public NodeDescription findById(String hostName) {
       return TopologyManager.getInstance().get(hostName);
    }

    @Override
    public List<NodeDescription> list() {
       return TopologyManager.getInstance().list();
    }

    @Override
    public NodeDescription save(NodeDescription node) {
        TopologyManager.getInstance().add(node);
        return node;
    }

    @Override
    public NodeDescription update(NodeDescription node) {
        TopologyManager.getInstance().update(node);
        return node;
    }

    @Override
    public void delete(String hostName) {
        TopologyManager.getInstance().remove(hostName);
    }

    @Override
    public List<Container> getDockerImages() {
        return TopologyManager.getInstance().getAvailableDockerImages();
    }

    @Override
    protected void validateFields(NodeDescription object, List<String> errors) throws ValidationException {
        String value = object.getHostName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[hostName] cannot be empty");
        }
        value = object.getUserName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[userName] cannot be empty");
        }
        value = object.getUserPass();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[password] cannot be empty");
        }
        if (object.getProcessorCount() <= 0) {
            errors.add("[processorCount] cannot be empty");
        }
        if (object.getMemorySizeGB() <= 0) {
            errors.add("[memorySize] cannot be empty");
        }
        if (object.getDiskSpaceSizeGB() <= 0) {
            errors.add("[diskSpace] cannot be empty");
        }
    }
}
