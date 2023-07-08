package io.wks.redisexperiment

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.zalando.jackson.datatype.money.MoneyModule


@Configuration
@EnableCaching
class RedisCachingConfiguration {

    @Bean
    fun cacheConfiguration(objectMapper: ObjectMapper): RedisCacheConfiguration? {
        val redisMapper = objectMapper.copy().also {
            it.findAndRegisterModules()
                .activateDefaultTyping(
                    it.polymorphicTypeValidator,
                    ObjectMapper.DefaultTyping.EVERYTHING,
                    JsonTypeInfo.As.PROPERTY
                )
        }

        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer(redisMapper)))
    }

    @Bean
    fun moneyModule(): MoneyModule {
        return MoneyModule()
    }
}
