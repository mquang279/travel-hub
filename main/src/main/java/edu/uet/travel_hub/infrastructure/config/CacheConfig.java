package edu.uet.travel_hub.infrastructure.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String CACHE_PROVINCES = "administrative:provinces";
    public static final String CACHE_DISTRICTS = "administrative:districts";
    public static final String CACHE_WARDS = "administrative:wards";
    public static final String CACHE_TRAVEL_PLACE_LISTS = "travel-places:lists";
    public static final String CACHE_TRAVEL_PLACE_RECOMMENDED = "travel-places:recommended";
    public static final String CACHE_TRAVEL_PLACE_ADMIN_DETAILS = "travel-places:admin-details";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();

        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "travel-hub:v4:" + cacheName + "::")
                .serializeValuesWith(SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> configurations = Map.of(
                CACHE_PROVINCES, defaultConfiguration.entryTtl(Duration.ofDays(1)),
                CACHE_DISTRICTS, defaultConfiguration.entryTtl(Duration.ofDays(1)),
                CACHE_WARDS, defaultConfiguration.entryTtl(Duration.ofDays(1)),
                CACHE_TRAVEL_PLACE_LISTS, defaultConfiguration.entryTtl(Duration.ofMinutes(15)),
                CACHE_TRAVEL_PLACE_RECOMMENDED, defaultConfiguration.entryTtl(Duration.ofMinutes(5)),
                CACHE_TRAVEL_PLACE_ADMIN_DETAILS, defaultConfiguration.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(configurations)
                .transactionAware()
                .build();
    }
}
