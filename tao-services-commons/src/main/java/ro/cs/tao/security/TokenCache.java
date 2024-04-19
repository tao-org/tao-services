package ro.cs.tao.security;

import org.springframework.security.core.Authentication;

public interface TokenCache extends TokenKeeper {

    default void put(Token token, Authentication authentication) { }

    default Authentication getAuthentication(String token) { return null; }

    default String getUser(String token) { return null; }

    default Authentication getPreviousAuthentication(String token) { return null; }
}
