/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.auth.token;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ro.cs.tao.security.Token;
import ro.cs.tao.security.TokenCache;
import ro.cs.tao.security.TokenProvider;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Oana H.
 */
@Component
//@Scope(value = "singleton")
public class TokenManagementService {
    private static final int REFRESH_INTERVAL = 300; // seconds
    private final Logger logger = Logger.getLogger(TokenManagementService.class.getName());
    private TokenCache tokenCache = new TokenCache() { };
    private TokenProvider tokenProvider = new TokenProvider() { };

    public void setTokenProvider(TokenProvider provider) {
        this.tokenProvider = provider;
    }

    public void setTokenCache(TokenCache tokenCache) {
        this.tokenCache = tokenCache;
    }

    @Scheduled(fixedRate = REFRESH_INTERVAL, timeUnit = TimeUnit.SECONDS)
    public void evictExpiredTokens() {
        logger.finest("Evicting expired tokens...");
        tokenCache.evictExpired();
    }

    public Token generateNewToken(String user, String password) {
        return tokenProvider.newToken(user, password);
    }

    public Token generateNewToken(String refreshToken) {
        return tokenProvider.newToken(refreshToken);
    }

    public Token generateNewTokenFromCode(String code) {
        return tokenProvider.newTokenFromCode(code);
    }

    public void store(Token token, Authentication authentication) {
        tokenCache.put(token, authentication);
    }

    public boolean isValid(String token) {
        final Authentication authentication = tokenCache.getAuthentication(token);
        if (authentication != null && authentication.isAuthenticated()) {
            return true;
        } else {
            final Token fullToken = tokenCache.getFullToken(token);
            if (fullToken != null) {
                return true;
            } else {
                return tokenProvider.validate(token);
            }
        }
    }

    public Authentication retrieve(String token) {
        return tokenCache.getAuthentication(token);
    }

    public Authentication retrieveFromRefreshToken(String token) {
        return tokenCache.getPreviousAuthentication(token);
    }

    public Token getUserToken(String userId) {
        return tokenCache.getToken(userId);
    }

    public Token getFromRefreshToken(String refreshToken) {
        return tokenCache.getFromRefreshToken(refreshToken);
    }

    public void removeUserTokens(String user) {
        tokenCache.remove(user);
    }

}
