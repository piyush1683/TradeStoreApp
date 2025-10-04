package com.sample.trade.capture;

import com.sample.trade.common.model.Trade;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class TradeCaptureServiceTest {
    @Test
    void consumingFromKafkaWritesToDynamoDb() {
        Trade t = new Trade();
        t.setTradeId("T2");
        t.setVersion(2);
        t.setCounterPartyId("CP-2");
        t.setBookId("B1");
        t.setMaturityDate(LocalDate.now().plusDays(2));
        t.setCreatedDate(LocalDate.now());
        t.setExpired("N");

        fail("Implement Kafka consumer and DynamoDB repository to persist trade");
    }
}



