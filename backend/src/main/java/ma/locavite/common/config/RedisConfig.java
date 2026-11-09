package ma.locavite.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration du cache Redis pour LocaVite avec sérialisation JSON sécurisée,
 * TTLs spécifiques, métriques Actuator et générateur de clé MD5.
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    public static final String CACHE_BRANDS = "brands";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_VEHICLE_SEARCH = "vehicleSearch";
    public static final String CACHE_VEHICLES = "vehicles";
    public static final String CACHE_VEHICLE_AVAILABILITY = "vehicleAvailability";

    @Bean
    @Primary
    @Override
    public KeyGenerator keyGenerator() {
        return new Md5KeyGenerator();
    }

    @Bean("md5KeyGenerator")
    public KeyGenerator md5KeyGenerator() {
        return keyGenerator();
    }

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper cacheObjectMapper = new ObjectMapper();
        cacheObjectMapper.registerModule(new JavaTimeModule());
        cacheObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cacheObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(Page.class, new PageDeserializer());
        Class<?> pageImplClass = PageImpl.class;
        pageModule.addDeserializer((Class) pageImplClass, new PageDeserializer());
        cacheObjectMapper.registerModule(pageModule);

        // Validation polymorphique restreinte aux packages applicatifs et utilitaires nécessaires
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("ma.locavite.")
                .allowIfSubType("java.util.")
                .allowIfSubType("org.springframework.data.domain.")
                .build();

        cacheObjectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();
        // GET /api/brands (TTL 1h)
        configurations.put(CACHE_BRANDS, defaultConfig.entryTtl(Duration.ofHours(1)));
        // GET /api/categories (TTL 1h)
        configurations.put(CACHE_CATEGORIES, defaultConfig.entryTtl(Duration.ofHours(1)));
        // GET /api/vehicles/search (TTL 5 min)
        configurations.put(CACHE_VEHICLE_SEARCH, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        // GET /api/vehicles/{id} (TTL 10 min)
        configurations.put(CACHE_VEHICLES, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configurations.put("vehicleById", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        // Disponibilité véhicule (TTL 1 min)
        configurations.put(CACHE_VEHICLE_AVAILABILITY, defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configurations)
                .initialCacheNames(Set.of(CACHE_BRANDS, CACHE_CATEGORIES, CACHE_VEHICLE_SEARCH, CACHE_VEHICLES, CACHE_VEHICLE_AVAILABILITY))
                .enableStatistics()
                .build();
    }
}
