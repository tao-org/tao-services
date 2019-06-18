package ro.cs.tao.services.entity.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.eodata.naming.NameExpressionParser;
import ro.cs.tao.eodata.naming.NameToken;
import ro.cs.tao.eodata.naming.NamingRule;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.NameTokenService;
import ro.cs.tao.services.model.component.NamingRuleTokens;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("nameTokenService")
public class NameTokenServiceImpl extends EntityService<NamingRule> implements NameTokenService {

    @Autowired
    private ComponentService componentService;
    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(ComponentService.class.getName());

    @Override
    public List<NamingRuleTokens> findTokens(long workflowNodeId) throws PersistenceException {
        List<NamingRuleTokens> tokens = new ArrayList<>();
        WorkflowNodeDescriptor node = persistenceManager.getWorkflowNodeById(workflowNodeId);
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
                    DataSourceComponent dsComponent = (DataSourceComponent) componentService.findComponent(ancestor.getComponentId(),
                                                                                                           ComponentType.DATASOURCE);
                    sensorName = dsComponent.getSensorName();
                    sensorTokens = new NamingRuleTokens();
                    sensorTokens.setSensor(sensorName);
                    sensorTokens.setMinIndex(current++);
                    sensorTokens.setMaxIndex(count > 1 ? current - 1 : Integer.MAX_VALUE);
                    sensorTokens.setTokens(getNameTokens(sensorName));
                    tokens.add(sensorTokens);
                    break;
                case DATASOURCE_GROUP:
                    DataSourceComponentGroup dsGroup = (DataSourceComponentGroup) componentService.findComponent(ancestor.getComponentId(),
                                                                                                                 ComponentType.DATASOURCE_GROUP);
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
        List<NamingRule> rules = persistenceManager.getRules(sensor);
        if (rules != null && rules.size() > 0) {
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
    public NamingRule findById(Integer id) throws PersistenceException {
        return persistenceManager.getRuleById(id);
    }

    @Override
    public List<NamingRule> list() {
        return persistenceManager.getAllRules();
    }

    @Override
    public List<NamingRule> list(Iterable<Integer> ids) {
        Set<Integer> idSet = new HashSet<>();
        for (Integer id : ids) {
            idSet.add(id);
        }
        return persistenceManager.getAllRules().stream()
                .filter(r -> idSet.contains(r.getId())).collect(Collectors.toList());
    }

    @Override
    public NamingRule save(NamingRule object) {
        try {
            return persistenceManager.saveRule(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public NamingRule update(NamingRule object) throws PersistenceException {
        return persistenceManager.saveRule(object);
    }

    @Override
    public void delete(Integer id) throws PersistenceException {
        persistenceManager.deleteRule(id);
    }

    @Override
    protected void validateFields(NamingRule entity, List<String> errors) {
        if (entity == null) {
            errors.add("[null entity]");
            return;
        }
        try {
            NameExpressionParser parser = new NameExpressionParser(entity);
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
