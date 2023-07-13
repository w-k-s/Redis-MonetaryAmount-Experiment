package io.wks.redisexperiment

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.javamoney.moneta.FastMoney
import org.springframework.data.redis.core.RedisHash
import java.util.*

@RedisHash("transaction")
class Transaction(
    @org.springframework.data.annotation.Id
    val id: Id,
    val description: String,
    val amount: FastMoney
) {
    class Id(val value: UUID) {
        companion object {
            fun random(): Id = Id(UUID.randomUUID())
        }
    }
}