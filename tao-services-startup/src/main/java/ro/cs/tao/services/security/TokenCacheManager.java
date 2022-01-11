package ro.cs.tao.services.security;

import ro.cs.tao.security.TokenCache;

public class TokenCacheManager {
    private static final TokenCache instance = new TokenCacheImpl();

    public static TokenCache getCache() { return instance; }
}
