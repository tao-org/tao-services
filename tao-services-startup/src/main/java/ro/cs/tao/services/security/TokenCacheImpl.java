package ro.cs.tao.services.security;

import org.springframework.security.core.Authentication;
import ro.cs.tao.Tuple;
import ro.cs.tao.security.Token;
import ro.cs.tao.security.TokenCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TokenCacheImpl implements TokenCache {
    private final Map<String, Tuple<Token, Authentication>> cache = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void evictExpired() {
        cache.entrySet().removeIf(e -> e.getValue().getKeyOne().isExpired());
    }

    @Override
    public void put(Token token, Authentication authentication) {
        cache.put(authentication.getPrincipal().toString(), new Tuple<>(token, authentication));
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