package ro.cs.tao.services.security;

import org.springframework.security.core.Authentication;
import ro.cs.tao.Tuple;
import ro.cs.tao.security.Token;
import ro.cs.tao.security.TokenCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TokenCacheImpl implements TokenCache {
    private final Map<String, Tuple<Token, Authentication>> cache = Collections.synchronizedMap(new HashMap<>());
    private final Logger logger = Logger.getLogger(TokenCache.class.getName());
    private final Map<String, Authentication> expiredTokens = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void evictExpired() {
        // Keep expired tokens only for one eviction cycle
        expiredTokens.clear();
        for (Map.Entry<String, Tuple<Token, Authentication>> next : cache.entrySet()) {
            if (next.getValue().getKeyOne().isExpired()) {
                logger.finest(String.format("Token %s for user %s expired", next.getValue().getKeyOne().getToken(), next.getKey()));
                //iterator.remove();
                expiredTokens.put(next.getValue().getKeyOne().getToken(), next.getValue().getKeyTwo());
            }
        }
    }

    @Override
    public void put(Token token, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Cannot cache a null authentication");
        }
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
        Authentication authentication = null;
        for (Map.Entry<String, Tuple<Token, Authentication>> entry : cache.entrySet()) {
            final Token t = entry.getValue().getKeyOne();
            if (!t.isExpired() && t.getToken().equals(token)) {
                authentication = entry.getValue().getKeyTwo();
            }
        }
        if (authentication == null) {
            // maybe it was a just expired token
            authentication = expiredTokens.get(token);
        }
        return authentication;
    }

    @Override
    public void remove(String user) {
        if (user != null) {
            cache.entrySet().removeIf(e -> user.equals(e.getKey()));
        }
    }
}