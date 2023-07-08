package io.wks.redisexperiment

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer

@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfiguration {

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> {
        return GenericContainer("redis:latest").withExposedPorts(6379)
    }

}

fun main(args: Array<String>) {
    fromApplication<RedisExperimentApplication>().with(TestContainersConfiguration::class).run(*args)
}
