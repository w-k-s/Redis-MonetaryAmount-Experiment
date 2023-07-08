package io.wks.redisexperiment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.javamoney.moneta.FastMoney
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.convert.DefaultRedisTypeMapper
import org.springframework.data.redis.core.convert.MappingRedisConverter
import org.springframework.data.redis.core.convert.RedisCustomConversions
import org.springframework.data.redis.core.convert.ReferenceResolver
import org.springframework.data.redis.core.mapping.RedisMappingContext
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.stereotype.Component
import java.util.*


@Configuration
@EnableRedisRepositories
class RedisRepositoriesConfiguration {

    @Bean
    fun redisConverter(
        mappingContext: RedisMappingContext?,
        customConversions: RedisCustomConversions?,
        referenceResolver: ReferenceResolver?
    ): MappingRedisConverter? {
        val mappingRedisConverter = MappingRedisConverter(
            mappingContext, null, referenceResolver,
            DefaultRedisTypeMapper()
        )
        mappingRedisConverter.setCustomConversions(redisCustomConversions())
        return mappingRedisConverter
    }


    @Bean
    fun redisCustomConversions(): RedisCustomConversions? {
        return RedisCustomConversions(
            mutableListOf(
                ByteArrayToTransactionIdConverter(), // MappingRedisConverter uses converters to convert Objects to Byte Array.
                TransactionIdToByteArrayConverter(),
                StringToTransactionIdConverter(), // MappingRedisConverter converts Id to type String
                TransactionIdToStringConverter(),
                // Custom Converter is required because MonetaryAmount uses java.util module which can't be opened by Reflection.
                ByteArrayToFastMoneyConverter(),
                FastMoneyToByteArrayConverter(),
            )
        )
    }
}

@Component
@ReadingConverter
class ByteArrayToTransactionIdConverter : Converter<ByteArray, Transaction.Id> {
    override fun convert(source: ByteArray) = source.toString(Charsets.UTF_8)
        .let(UUID::fromString)
        .let(Transaction::Id)
}

@Component
@WritingConverter
class TransactionIdToByteArrayConverter : Converter<Transaction.Id, ByteArray> {
    override fun convert(source: Transaction.Id) = source.value.toString().toByteArray()
}

@Component
@ReadingConverter
class StringToTransactionIdConverter : Converter<String, Transaction.Id> {
    override fun convert(source: String) = Transaction.Id(UUID.fromString(source))
}

@Component
@WritingConverter
class TransactionIdToStringConverter : Converter<Transaction.Id, String> {
    override fun convert(source: Transaction.Id) = source.value.toString()
}

@Component
@WritingConverter
class ByteArrayToFastMoneyConverter : Converter<ByteArray, FastMoney> {
    override fun convert(source: ByteArray) = FastMoney.parse(source.toString(Charsets.UTF_8))
}

@Component
@WritingConverter
class FastMoneyToByteArrayConverter : Converter<FastMoney, ByteArray> {
    override fun convert(source: FastMoney) = source.toString().toByteArray(Charsets.UTF_8)
}