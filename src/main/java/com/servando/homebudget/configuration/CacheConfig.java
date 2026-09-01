package com.servando.homebudget.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "subscriptions", "bills", "credit-cards", "debts", "house-repairs", "incomes"
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
        );
        return manager;
    }
}