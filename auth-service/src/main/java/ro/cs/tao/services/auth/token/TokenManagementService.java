/*
 * Copyright (C) 2017 CS ROMANIA
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 *
 * @author Oana H.
 */
@Component
@Scope(value = "singleton")
public class TokenManagementService {
    private static final Logger logger = LoggerFactory.getLogger(TokenManagementService.class);
    private static final Cache authTokenCache = CacheManager.getInstance().getCache("authTokenCache");
    public static final int ONE_HOUR_IN_MILLISECONDS = 60 * 60 * 1000;

    @Scheduled(fixedRate = ONE_HOUR_IN_MILLISECONDS)
    public void evictExpiredTokens() {
        logger.info("Evicting expired tokens...");
        authTokenCache.evictExpiredElements();
    }

    public String generateNewToken() {
        return UUID.randomUUID().toString();
    }

    public void store(String token, Authentication authentication) {
        authTokenCache.put(new Element(token, authentication));
    }

    public boolean contains(String token) {
        return authTokenCache.get(token) != null;
    }

    public Authentication retrieve(String token) {
        return (Authentication) authTokenCache.get(token).getObjectValue();
    }

    public String getUserToken(String username){
        for (Object key: authTokenCache.getKeys()) {
            Element element = authTokenCache.get(key);
            if (element != null && ((Authentication)element.getObjectValue()).getPrincipal().toString().equals(username)) {
                return key.toString();
            }
        }
        return null;
    }
}
