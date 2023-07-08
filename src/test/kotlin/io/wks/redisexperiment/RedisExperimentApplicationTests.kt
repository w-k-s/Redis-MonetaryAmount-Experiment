package io.wks.redisexperiment

import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.FastMoney
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate

@SpringBootTest
@Import(TestContainersConfiguration::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisExperimentApplicationTests {

    @Test
    fun contextLoads() {
    }

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var endOfDayBalanceService: EndOfDayBalanceService

    @Test
    fun `GIVEN transaction WHEN saved THEN can be retrieved`() {
        // GIVEN
        val id = Transaction.Id.random()
        val transaction = Transaction(
            id = id,
            description = "Thing",
            amount = FastMoney.of(10, "SAR")
        )

        // WHEN
        transactionRepository.save(transaction)

        // THEN
        val actual = transactionRepository.findById(id).get()
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(transaction)
    }

    @Test
    fun `GIVEN date WHEN eod balance calculated and cached THEN calculation not recalculated`() {
        // GIVEN
        val date = LocalDate.now()

        // WHEN
        endOfDayBalanceService.endOfDayBalance(date)
        val uncachedInvocation = endOfDayBalanceService.invocations()

        endOfDayBalanceService.endOfDayBalance(date)
        val cachedInvocation = endOfDayBalanceService.invocations()

        assertThat(cachedInvocation)
            .isEqualTo(uncachedInvocation)
    }
}
