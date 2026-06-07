package com.sprint.analyzer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.sprint.analyzer.service.FeatureFlagService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CachingConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(FeatureFlagService.CACHE_NAME);
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)   // hard refresh every minute
                .maximumSize(500));
        return mgr;
    }
}