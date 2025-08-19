package com.rahman.productservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Objects;

/**
 * Base class untuk service-level integration test.
 * Extend ini kalau mau ada assert cache helper.
 */
public abstract class BaseServiceIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    protected CacheManager cacheManager;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void clearRedis() {
        try (var connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    /**
     * Ambil cache berdasarkan nama.
     */
    protected Cache getCache(String name) {
        return Objects.requireNonNull(cacheManager.getCache(name),
                () -> "Cache with name [" + name + "] not found");
    }

    /**
     * Assert bahwa cache dengan key tertentu sudah dihapus.
     */
    protected void assertCacheEvicted(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        Object value = cache.get(key, Object.class);
        if (value != null) {
            throw new AssertionError("Expected cache [" + cacheName + "] with key [" + key + "] to be evicted, but found value: " + value);
        }
    }

    /**
     * Assert bahwa cache dengan key tertentu ada isinya.
     */
    protected void assertCachePresent(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        Object value = cache.get(key, Object.class);
        if (value == null) {
            throw new AssertionError("Expected cache [" + cacheName + "] with key [" + key + "] to be present, but was null");
        }
    }


}
