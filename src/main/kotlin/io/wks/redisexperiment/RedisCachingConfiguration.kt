package io.wks.redisexperiment

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair


@Configuration
@EnableCaching
class RedisCachingConfiguration {
    @Bean
    fun cacheConfiguration(redisTemplate: RedisTemplate<Any, Any>): RedisCacheConfiguration? {
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(SerializationPair.fromSerializer(redisTemplate.valueSerializer))
    }
}