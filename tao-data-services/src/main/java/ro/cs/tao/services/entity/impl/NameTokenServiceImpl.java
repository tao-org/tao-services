package ro.cs.tao.services.entity.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.eodata.naming.NameExpressionParser;
import ro.cs.tao.eodata.naming.NameToken;
import ro.cs.tao.eodata.naming.NamingRule;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.NameTokenService;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("nameTokenService")
public class NameTokenServiceImpl extends EntityService<NamingRule> implements NameTokenService {

    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(ComponentService.class.getName());

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
