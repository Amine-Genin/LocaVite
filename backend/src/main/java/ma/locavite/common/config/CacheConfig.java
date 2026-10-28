package ma.locavite.common.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

@Configuration
public class CacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer vehicleSearchCacheCustomizer() {
        return builder -> builder.withCacheConfiguration(
                "vehicleSearch",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
    }
}
