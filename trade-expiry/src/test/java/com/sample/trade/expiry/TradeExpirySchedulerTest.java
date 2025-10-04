package com.sample.trade.expiry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class TradeExpirySchedulerTest {
    @Test
    void schedulerMarksExpiredTradesInPostgres() {
        fail("Implement scheduled job that updates expired flag in Postgres");
    }
}



