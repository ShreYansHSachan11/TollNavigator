package com.tollplaza.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfiguration.class);

    @Bean
    public CacheManager cacheManager() {
        logger.info("Initializing cache manager with Caffeine");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("tollPlazaRoutes");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(24, TimeUnit.HOURS));
        logger.info("Cache manager configured: max size=1000, TTL=24 hours");
        return cacheManager;
    }
}
