package io.wks.redisexperiment

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.springframework.data.redis.serializer.RedisSerializer


@Configuration
@EnableCaching
class RedisCachingConfiguration {

    @Bean
    fun cacheConfiguration(objectMapper: ObjectMapper): RedisCacheConfiguration? {
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(SerializationPair.fromSerializer(RedisSerializer.java()))
    }
}
