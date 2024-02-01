package ro.cs.tao.services.security;

import org.springframework.security.core.Authentication;
import ro.cs.tao.security.Token;
import ro.cs.tao.security.TokenCache;
import ro.cs.tao.utils.Tuple;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class TokenCacheImpl implements TokenCache {
    private final Map<String, Tuple<Token, Authentication>> cache = new HashMap<>();
    private final Logger logger = Logger.getLogger(TokenCache.class.getName());
    private final Map<Token, Authentication> expiredTokens = new HashMap<>();

    @Override
    public void evictExpired() {
        // Keep expired tokens only for one eviction cycle
        synchronized (cache) {
            expiredTokens.clear();
            final Iterator<Map.Entry<String, Tuple<Token, Authentication>>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Tuple<Token, Authentication>> next = iterator.next();
                if (next.getValue().getKeyOne().isExpired()) {
                    logger.finest(String.format("Token %s for user %s expired", next.getValue().getKeyOne().getToken(), next.getKey()));
                    expiredTokens.put(next.getValue().getKeyOne(), next.getValue().getKeyTwo());
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void put(Token token, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warning(String.format("Token %s has no corresponding authentication", token.getToken()));
        } else {
            /*Principal principal = null;
            if (authentication instanceof AuthenticationWithToken) {
                principal = ((AuthenticationWithToken) authentication).getLoginContext().getSubject().getPrincipals().stream().findFirst().orElse(null);
            }
            final String user = principal != null ? principal.getName() : authentication.getPrincipal().toString();*/
            cache.put(authentication.getPrincipal().toString(), new Tuple<>(token, authentication));
        }
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
    public String getUser(String token) {
        final Map.Entry<String, Tuple<Token, Authentication>> entry = cache.entrySet().stream().filter(e -> e.getValue().getKeyOne().getToken()
                                                                                                             .equals(token)).findFirst().orElse(null);
        return entry != null ? entry.getKey() : null;
    }

    @Override
    public Token getFromRefreshToken(String refreshToken) {
        return expiredTokens.keySet().stream().filter(t -> t.getRefreshToken().equals(refreshToken)).findFirst().orElse(null);
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
            if ((!t.isExpired() && t.getToken().equals(token)) ||
                    (t.getRefreshToken() != null && t.getRefreshToken().equals(token))) {
                authentication = entry.getValue().getKeyTwo();
            }
        }
        /*if (authentication == null) {
            // maybe it was a just expired token
            final Map.Entry<Token, Authentication> entry = expiredTokens.entrySet().stream().filter(e -> e.getKey().getRefreshToken().equals(token)).findFirst().orElse(null);
            return entry != null ? entry.getValue() : null;
        }*/
        return authentication;
    }

    @Override
    public Authentication getPreviousAuthentication(String token) {
        final Map.Entry<String, Tuple<Token, Authentication>> tuple = cache.entrySet().stream().filter(e -> e.getValue().getKeyOne().getRefreshToken().equals(token)).findFirst().orElse(null);
        if (tuple != null) {
            return tuple.getValue().getKeyTwo();
        } else {
            // maybe it was a just expired token
            final Map.Entry<Token, Authentication> entry = expiredTokens.entrySet().stream().filter(e -> e.getKey().getRefreshToken().equals(token)).findFirst().orElse(null);
            return entry != null ? entry.getValue() : null;
        }
    }

    @Override
    public void remove(String user) {
        if (user != null) {
            cache.entrySet().removeIf(e -> user.equals(e.getKey()));
        }
    }
}