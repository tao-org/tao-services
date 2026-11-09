package ro.cs.tao.security;

import org.springframework.security.core.Authentication;

import java.util.function.BiFunction;

/**
 * The TokenCache interface provides methods to manage the caching of
 * tokens and their associated authentication details. It extends the
 * TokenKeeper interface and includes additional functions for retrieving
 * and managing authentication and user information linked to tokens.
 */
public interface TokenCache extends TokenKeeper {

    default void put(Token token, Authentication authentication) { }

    default Authentication getAuthentication(String token) { return null; }

    default String getUser(String token) { return null; }

    default Authentication getPreviousAuthentication(String token) { return null; }

    default void setTokenProvider(TokenProvider tokenProvider) { }

    default void setAuthenticationFactory(BiFunction<String, String, Authentication> function) { }

    default void addAuthenticationFactory(BiFunction<String, String, Authentication> function) { }
}
