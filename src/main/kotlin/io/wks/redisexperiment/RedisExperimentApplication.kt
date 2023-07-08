package io.wks.redisexperiment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RedisExperimentApplication

fun main(args: Array<String>) {
    runApplication<RedisExperimentApplication>(*args)
}
