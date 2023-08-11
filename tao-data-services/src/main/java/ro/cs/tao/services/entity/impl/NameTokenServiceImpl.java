package ro.cs.tao.services.entity.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.eodata.naming.NameExpressionParser;
import ro.cs.tao.eodata.naming.NameToken;
import ro.cs.tao.eodata.naming.NamingRule;
import ro.cs.tao.persistence.NamingRuleProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.WorkflowNodeProvider;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.NameTokenService;
import ro.cs.tao.services.model.component.NamingRuleTokens;
import ro.cs.tao.services.utils.WorkflowUtilities;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("nameTokenService")
public class NameTokenServiceImpl extends EntityService<NamingRule> implements NameTokenService {

    @Autowired
    private ComponentService componentService;
    @Autowired
    private WorkflowNodeProvider workflowNodeProvider;
    @Autowired
    private NamingRuleProvider namingRuleProvider;

    @Override
    public List<NamingRuleTokens> findTokens(long workflowNodeId) throws PersistenceException {
        List<NamingRuleTokens> tokens = new ArrayList<>();
        WorkflowNodeDescriptor node = workflowNodeProvider.get(workflowNodeId);
        if (node == null) {
            throw new PersistenceException(String.format("Workflow node with id=%d does not exist", workflowNodeId));
        }
        WorkflowDescriptor workflow = node.getWorkflow();
        List<WorkflowNodeDescriptor> ancestors = workflow.findAncestors(workflow.getOrderedNodes(), node);
        ancestors.removeIf(a -> a.getComponentType() != ComponentType.DATASOURCE && a.getComponentType() != ComponentType.DATASOURCE_GROUP);
        final int count = ancestors.size();
        String sensorName;
        NamingRuleTokens sensorTokens;
        int current = 1;
        for (WorkflowNodeDescriptor ancestor : ancestors) {
            switch (ancestor.getComponentType()) {
                case DATASOURCE:
                    DataSourceComponent dsComponent = (DataSourceComponent) WorkflowUtilities.findComponent(ancestor);
                    sensorName = dsComponent.getSensorName();
                    sensorTokens = new NamingRuleTokens();
                    sensorTokens.setSensor(sensorName);
                    sensorTokens.setMinIndex(current++);
                    sensorTokens.setMaxIndex(count > 1 ? current - 1 : Integer.MAX_VALUE);
                    sensorTokens.setTokens(getNameTokens(sensorName));
                    tokens.add(sensorTokens);
                    break;
                case DATASOURCE_GROUP:
                    DataSourceComponentGroup dsGroup = (DataSourceComponentGroup) WorkflowUtilities.findComponent(ancestor);
                    List<DataSourceComponent> dataSourceComponents = dsGroup.getDataSourceComponents();
                    final int groupCount = dataSourceComponents.size();
                    for (DataSourceComponent component : dataSourceComponents) {
                        sensorName = component.getSensorName();
                        sensorTokens = new NamingRuleTokens();
                        sensorTokens.setSensor(sensorName);
                        sensorTokens.setMinIndex(current++);
                        sensorTokens.setMaxIndex(count > 1 || groupCount > 1 ? current - 1 : Integer.MAX_VALUE);
                        sensorTokens.setTokens(getNameTokens(sensorName));
                        tokens.add(sensorTokens);
                    }
                    break;
                default:
                    break;
            }
        }
        return tokens;
    }

    @Override
    public Map<String, String> getNameTokens(String sensor) {
        final Map<String, String> tokens = new LinkedHashMap<>();
        List<NamingRule> rules = namingRuleProvider.listBySensor(sensor);
        if (rules != null && !rules.isEmpty()) {
            NamingRule first = rules.get(0);
            final List<NameToken> tokenList = first.getTokens();
            tokenList.sort(Comparator.comparingInt(NameToken::getMatchingGroupNumber));
            for (NameToken token : tokenList) {
                tokens.put(token.getName(), token.getDescription());
            }
        }
        return tokens;
    }

    @Override
    public NamingRule findById(Integer id) {
        return namingRuleProvider.get(id);
    }

    @Override
    public List<NamingRule> list() {
        return namingRuleProvider.list();
    }

    @Override
    public List<NamingRule> list(Iterable<Integer> ids) {
        Set<Integer> idSet = new HashSet<>();
        for (Integer id : ids) {
            idSet.add(id);
        }
        return namingRuleProvider.list().stream()
                .filter(r -> idSet.contains(r.getId())).collect(Collectors.toList());
    }

    @Override
    public NamingRule save(NamingRule object) {
        try {
            return namingRuleProvider.save(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public NamingRule update(NamingRule object) throws PersistenceException {
        return namingRuleProvider.save(object);
    }

    @Override
    public void delete(Integer id) throws PersistenceException {
        namingRuleProvider.delete(id);
    }

    @Override
    protected void validateFields(NamingRule entity, List<String> errors) {
        if (entity == null) {
            errors.add("[null entity]");
            return;
        }
        try {
            new NameExpressionParser(entity);
        } catch (Exception e) {
            errors.add("[regex] " + e.getMessage());
        }
        if (StringUtils.isBlank(entity.getRegEx())) {
            errors.add("[regex] field cannot be empty");
        }
        if (StringUtils.isBlank(entity.getSensor())) {
            errors.add("[sensor] field cannot be empty");
        }
        if (StringUtils.isBlank(entity.getDescription())) {
            errors.add("[description] field cannot be empty");
        }
    }
}
