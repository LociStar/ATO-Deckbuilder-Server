package com.loci.ato_deck_builder_server.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(10_000);
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("usernames", "userIds");
        cacheManager.setCaffeine(caffeine);
        cacheManager.setAllowNullValues(false); // Optional: Disallow caching null values
        cacheManager.setAsyncCacheMode(true);   // Enable asynchronous caching
        return cacheManager;
    }
}
