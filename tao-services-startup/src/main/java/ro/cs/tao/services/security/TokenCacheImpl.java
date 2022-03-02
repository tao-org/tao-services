package ro.cs.tao.services.security;

import org.springframework.security.core.Authentication;
import ro.cs.tao.Tuple;
import ro.cs.tao.security.Token;
import ro.cs.tao.security.TokenCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class TokenCacheImpl implements TokenCache {
    private final Map<String, Tuple<Token, Authentication>> cache = Collections.synchronizedMap(new HashMap<>());
    private final Logger logger = Logger.getLogger(TokenCache.class.getName());

    @Override
    public void evictExpired() {
        final Iterator<Map.Entry<String, Tuple<Token, Authentication>>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, Tuple<Token, Authentication>> next = iterator.next();
            if (next.getValue().getKeyOne().isExpired()) {
                logger.finest(String.format("Token %s for user %s expired", next.getValue().getKeyOne().getToken(), next.getKey()));
                iterator.remove();
            }
        }
    }

    @Override
    public void put(Token token, Authentication authentication) {
        final String user = authentication.getPrincipal().toString();
        cache.put(user, new Tuple<>(token, authentication));
        //logger.finest(String.format("Token %s for user %s cached", token.getToken(), user));
    }

    @Override
    public void put(Token token, String user) {
        cache.put(user, new Tuple<>(token, null));
    }

    @Override
    public Token getToken(String user) {
        final Tuple<Token, Authentication> tuple = cache.get(user);
        return tuple != null ? tuple.getKeyOne() : null;
    }

    @Override
    public Token getFullToken(String token) {
        return cache.values().stream()
                    .filter(t -> t.getKeyOne().getToken().equals(token) && !t.getKeyOne().isExpired())
                    .findFirst().map(Tuple::getKeyOne).orElse(null);
    }

    @Override
    public Authentication getAuthentication(String token) {
        for (Map.Entry<String, Tuple<Token, Authentication>> entry : cache.entrySet()) {
            final Token t = entry.getValue().getKeyOne();
            if (!t.isExpired() && t.getToken().equals(token)) {
                return entry.getValue().getKeyTwo();
            }
        }
        return null;
    }

    @Override
    public void remove(String user) {
        if (user != null) {
            cache.entrySet().removeIf(e -> user.equals(e.getKey()));
        }
    }
}