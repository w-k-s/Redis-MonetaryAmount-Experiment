package io.wks.redisexperiment

import org.javamoney.moneta.FastMoney
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.money.MonetaryAmount

@Service
class EndOfDayBalanceService {

    private val counter: AtomicInteger = AtomicInteger()

    @Cacheable(value = ["eod-balance"])
    fun endOfDayBalance(date: LocalDate): MonetaryAmount {
        val result = FastMoney.of(date.year + date.month.value + date.dayOfMonth, "AED")
        counter.incrementAndGet()
        return result
    }

    fun invocations(): Int{
        return counter.get()
    }
}