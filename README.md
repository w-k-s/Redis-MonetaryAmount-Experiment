# Redis MonetaryAmount Experiment

The purpose of this repository is to investigate issues I faced saving instances
of `javax.money.MonetaryAmount` to Redis using `spring-data-redis`.

`javax.money.MonetaryAmount` is an interface described in JSR 354 (Money and Currency API).
We were using it's, as far as I can tell, one and only implementation: [`Moneta`](https://github.com/JavaMoney/jsr354-ri).


## Redis Repositories

For the sake of flexibility, our cadebase used `javax.money.MonetaryAmount` as the type throughout the codebase and we would pass an instance of `org.javamoney.moneta.FastMoney`.

```kotlin
data class Example(val amount: MonetaryAmount)
val example = Example(FastMoney.of(1, "USD"))
```

The problem was that when saving such an object to a RedisRepository,  the following exception is thrown:

```text
org.springframework.data.keyvalue.core.UncategorizedKeyValueException: No getter available for persistent property javax.money.MonetaryAmountFactory.number

	at org.springframework.data.keyvalue.core.KeyValuePersistenceExceptionTranslator.translateExceptionIfPossible(KeyValuePersistenceExceptionTranslator.java:51)
	at org.springframework.data.keyvalue.core.KeyValueTemplate.resolveExceptionIfPossible(KeyValueTemplate.java:405)
	....
Caused by: java.lang.IllegalArgumentException: No getter available for persistent property javax.money.MonetaryAmountFactory.number
	at org.springframework.data.mapping.PersistentProperty.getRequiredGetter(PersistentProperty.java:93)
	at org.springframework.data.mapping.model.BeanWrapper.getProperty(BeanWrapper.java:135)
	at org.springframework.data.mapping.model.BeanWrapper.getProperty(BeanWrapper.java:108)
	at org.springframework.data.redis.core.convert.PathIndexResolver$1.doWithPersistentProperty(PathIndexResolver.java:122)
	...
	at org.springframework.data.redis.core.convert.MappingRedisConverter.write(MappingRedisConverter.java:114)
	at org.springframework.data.redis.core.RedisKeyValueAdapter.put(RedisKeyValueAdapter.java:195)
	at org.springframework.data.keyvalue.core.KeyValueTemplate.lambda$update$1(KeyValueTemplate.java:201)
	at org.springframework.data.keyvalue.core.KeyValueTemplate.execute(KeyValueTemplate.java:314)
```

From my understanding, the error occurs because redis tries to index each field in MonetaryAmount.
As MonetaryAmount is an interface, that means it calls each getter in the interface, of them being MonetaryAmountFactory getFactory().
MonetaryAmountFactory is an interface that contains method to setNumber, but none to getNumber which I assume is the cause of this error.

From the Spring Redis Documentation, I should be able to completely customise the indexing by using a converter of type `Converter<MonetaryAmount,Map<String,ByteArray>>` instead of `Converter<MonetaryAmount,ByteArray>`
but this resulted in the same error.

I tried to get help on [StackOverflow](https://stackoverflow.com/q/76610499/821110) and [spring-data-redis'](https://github.com/spring-projects/spring-data-redis/issues/2630) repository but to no avail.

The only way it works is if `org.javamoney.moneta.FastMoney` is used throughout along with a custom Converter.
The custom converter is required because without it the following exception occurs:

```text
org.springframework.data.keyvalue.core.UncategorizedKeyValueException: Unable to make private java.util.Currency(java.lang.String,int,int) accessible: module java.base does not "opens java.util" to unnamed module @5a39699c
	at org.springframework.data.keyvalue.core.KeyValuePersistenceExceptionTranslator.translateExceptionIfPossible(KeyValuePersistenceExceptionTranslator.java:51)
	at org.springframework.data.keyvalue.core.KeyValueTemplate.resolveExceptionIfPossible(KeyValueTemplate.java:405)
	at org.springframework.data.keyvalue.core.KeyValueTemplate.execute(KeyValueTemplate.java:316)
	...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make private java.util.Currency(java.lang.String,int,int) accessible: module java.base does not "opens java.util" to unnamed module @5a39699c
	at java.base/java.lang.reflect.AccessibleObject.checkCanSetAccessible(AccessibleObject.java:354)
	at java.base/java.lang.reflect.AccessibleObject.checkCanSetAccessible(AccessibleObject.java:297)
	at java.base/java.lang.reflect.Constructor.checkCanSetAccessible(Constructor.java:188)
	...
```

---

## Caching

Caching objects containing `MonetaryAmount` (using `@Cacheable`) proved to be a challenge to when using the `GenericJackson2JsonRedisSerializer` 
(The default `JdkSerializationRedisSerializer` works fine since `FastMoney` is `Serializable` ).
The following exception was observed:

```
org.springframework.data.redis.serializer.SerializationException: Could not read JSON:Cannot construct instance of `org.javamoney.moneta.FastMoney` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)
 at [Source: (byte[])"{"@class":"org.javamoney.moneta.FastMoney","currency":{"@class":"org.javamoney.moneta.spi.JDKCurrencyAdapter","context":{"empty":false,"providerName":"java.util.Currency"},"defaultFractionDigits":2,"currencyCode":"AED","numericCode":784},"number":["org.javamoney.moneta.spi.DefaultNumberValue",2038.00000],"factory":{"@class":"org.javamoney.moneta.spi.FastMoneyAmountFactory","defaultMonetaryContext":{"precision":19,"maxScale":5,"fixedScale":true,"amountType":"org.javamoney.moneta.FastMoney","empty"[truncated 645 bytes]; line: 1, column: 44] 

	at org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer.deserialize(GenericJackson2JsonRedisSerializer.java:253)
	...
Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Cannot construct instance of `org.javamoney.moneta.FastMoney` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)
 at [Source: (byte[])"{"@class":"org.javamoney.moneta.FastMoney","currency":{"@class":"org.javamoney.moneta.spi.JDKCurrencyAdapter","context":{"empty":false,"providerName":"java.util.Currency"},"defaultFractionDigits":2,"currencyCode":"AED","numericCode":784},"number":["org.javamoney.moneta.spi.DefaultNumberValue",2038.00000],"factory":{"@class":"org.javamoney.moneta.spi.FastMoneyAmountFactory","defaultMonetaryContext":{"precision":19,"maxScale":5,"fixedScale":true,"amountType":"org.javamoney.moneta.FastMoney","empty"[truncated 645 bytes]; line: 1, column: 44]
	at com.fasterxml.jackson.databind.exc.InvalidDefinitionException.from(InvalidDefinitionException.java:67)
	...
```

The issue here is that FastMoney does not provide a no-arg constructor so Jackson doesn't know how to create an instance of it when deserializing.
To resolve this, a custom deserializer is required. Zalando provides a library that contains custom serdes for Java Money types: `org.zalando:jackson-datatype-money`.

```kotlin
@Bean
fun cacheConfiguration(objectMapper: ObjectMapper): RedisCacheConfiguration {
    val redisMapper = objectMapper.copy().also {
        it.findAndRegisterModules()                 // Finds and registers MoneyModule() from org.zalando:jackson-datatype-money
            .activateDefaultTyping(                 // GenericJackson2JsonRedisSerializer requires serialization to be done with type info.
                it.polymorphicTypeValidator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )
    }

    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(60))
        .disableCachingNullValues()
        .serializeValuesWith(SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer(redisMapper)))
}    
```

However the deserializer from Zalando did not work because the serializer does not include type info when serializing.
```text
org.springframework.data.redis.serializer.SerializationException: Could not write JSON: Type id handling not implemented for type javax.money.CurrencyUnit (by serializer of type org.zalando.jackson.datatype.money.CurrencyUnitSerializer)

	at org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer.serialize(GenericJackson2JsonRedisSerializer.java:223)
	at org.springframework.data.redis.serializer.DefaultRedisElementWriter.write(DefaultRedisElementWriter.java:41)
	at org.springframework.data.redis.serializer.RedisSerializationContext$SerializationPair.write(RedisSerializationContext.java:287)
	...
Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Type id handling not implemented for type javax.money.CurrencyUnit (by serializer of type org.zalando.jackson.datatype.money.CurrencyUnitSerializer)
	at com.fasterxml.jackson.databind.exc.InvalidDefinitionException.from(InvalidDefinitionException.java:77)
	at com.fasterxml.jackson.databind.SerializerProvider.reportBadDefinition(SerializerProvider.java:1308)
	... 84 more
```

I can't f**king figure out how to write a typed serializer either!


