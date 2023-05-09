package ro.cs.tao.security;

import org.springframework.security.core.Authentication;

public interface TokenCache {
    default void evictExpired() { }

    default void put(Token token, Authentication authentication) { }

    default void put(Token token, String user) { }

    default Token getToken(String user) { return null; }

    default Token getFromRefreshToken(String refreshToken) { return null; }

    default Token getFullToken(String token) { return null; }

    default Authentication getAuthentication(String token) { return null; }

    default Authentication getPreviousAuthentication(String token) { return null; }

    default void remove(String user) { }
}
