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
import ro.cs.tao.Tag;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.docker.Container;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.TopologyService;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.NodeFlavor;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.topology.docker.DockerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Cosmin Cara
 */
@Service("topologyService")
public class TopologyServiceImpl
    extends EntityService<NodeDescription>
        implements TopologyService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public NodeDescription findById(String hostName) {
       return TopologyManager.getInstance().getNode(hostName);
    }

    @Override
    public List<NodeDescription> list() {
       return TopologyManager.getInstance().listNodes();
    }

    @Override
    public List<NodeDescription> list(Iterable<String> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        Set<String> identifiers = StreamSupport.stream(ids.spliterator(), false).collect(Collectors.toSet());
        return list().stream().filter(n -> identifiers.contains(n.getId())).collect(Collectors.toList());
    }

    @Override
    public List<NodeDescription> getNodes(boolean active) {
        return persistenceManager.getNodes(active);
    }

    @Override
    public List<NodeDescription> getNodes(NodeFlavor nodeFlavor) {
        return persistenceManager.getNodesByType(nodeFlavor);
    }

    @Override
    public NodeDescription save(NodeDescription node) {
        List<String> nodeTags = node.getTags();
        if (nodeTags != null) {
            if (!nodeTags.contains(node.getFlavor().getId())) {
                nodeTags.add(node.getFlavor().getId());
            }
            Set<String> existingTags = persistenceManager.getNodeTags().stream().map(Tag::getText).collect(Collectors.toSet());
            for (String value : nodeTags) {
                if (!existingTags.contains(value)) {
                    persistenceManager.saveTag(new Tag(TagType.TOPOLOGY_NODE, value));
                }
            }
        }
        TopologyManager.getInstance().addNode(node);
        return node;
    }

    @Override
    public NodeDescription update(NodeDescription node) {
        NodeDescription existing = findById(node.getId());
        if (existing != null && !existing.getFlavor().equals(node.getFlavor())) {
            List<String> nodeTags = node.getTags();
            if (nodeTags != null) {
                if (!nodeTags.contains(node.getFlavor().getId())) {
                    nodeTags.add(node.getFlavor().getId());
                }
                Set<String> existingTags = persistenceManager.getNodeTags().stream().map(Tag::getText).collect(Collectors.toSet());
                for (String value : nodeTags) {
                    if (!existingTags.contains(value)) {
                        persistenceManager.saveTag(new Tag(TagType.TOPOLOGY_NODE, value));
                    }
                }
            }
        }
        TopologyManager.getInstance().updateNode(node);
        return node;
    }

    @Override
    public void delete(String hostName) {
        TopologyManager.getInstance().removeNode(hostName);
    }

    @Override
    public NodeDescription tag(String id, List<String> tags) throws PersistenceException {
        NodeDescription entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Node with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            Set<String> existingTags = persistenceManager.getNodeTags().stream()
                    .map(Tag::getText).collect(Collectors.toSet());
            for (String value : tags) {
                if (!existingTags.contains(value)) {
                    persistenceManager.saveTag(new Tag(TagType.TOPOLOGY_NODE, value));
                }
            }
            entity.setTags(tags);
            return update(entity);
        }
        return entity;
    }

    @Override
    public NodeDescription untag(String id, List<String> tags) throws PersistenceException {
        NodeDescription entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Node with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            List<String> entityTags = entity.getTags();
            if (entityTags != null) {
                for (String value : tags) {
                    entityTags.remove(value);
                }
                entity.setTags(entityTags);
                //return update(entity);
            }
        }
        return entity;
    }

    @Override
    public List<Container> getDockerImages() {
        return DockerManager.getAvailableDockerImages();
    }

    @Override
    public List<Tag> getNodeTags() {
        return persistenceManager.getNodeTags();
    }

    @Override
    protected void validateFields(NodeDescription object, List<String> errors) throws ValidationException {
        String value = object.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        value = object.getUserName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[userName] cannot be empty");
        }
        value = object.getUserPass();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[password] cannot be empty");
        }
        final NodeFlavor flavor = object.getFlavor();
        if (flavor == null) {
            errors.add("[flavor] cannot be null");
        } else {
            if (flavor.getCpu() <= 0) {
                errors.add("[flavor.cpu] cannot be less than 1");
            }
            if (flavor.getMemory() <= 0) {
                errors.add("[flavor.memory] cannot be less than 1");
            }
            if (flavor.getDisk() <= 0) {
                errors.add("[flavor.disk] cannot be less than 1");
            }
            if (Float.compare(flavor.getRxtxFactor(), 0.0f) <= 0) {
                errors.add("[flavor.rxtxFactor] must be positive");
            }
        }
    }
}
