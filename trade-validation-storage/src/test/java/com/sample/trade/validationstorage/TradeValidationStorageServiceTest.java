package com.sample.trade.validationstorage;

import com.sample.trade.common.model.Trade;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TradeValidationStorageServiceTest {
    @Test
    void rejectsLowerVersionAndPastMaturity_thenStoresToPostgres() {
        Trade lowerVersion = new Trade();
        lowerVersion.setTradeId("T2");
        lowerVersion.setVersion(1);
        lowerVersion.setCounterPartyId("CP-1");
        lowerVersion.setBookId("B1");
        lowerVersion.setMaturityDate(LocalDate.now().plusDays(1));
        lowerVersion.setCreatedDate(LocalDate.now());
        lowerVersion.setExpired("N");

        Trade pastMaturity = new Trade();
        pastMaturity.setTradeId("T3");
        pastMaturity.setVersion(3);
        pastMaturity.setCounterPartyId("CP-3");
        pastMaturity.setBookId("B2");
        pastMaturity.setMaturityDate(LocalDate.now().minusDays(1));
        pastMaturity.setCreatedDate(LocalDate.now());
        pastMaturity.setExpired("N");

        fail("Implement validation against DynamoDB stream event and persist valid trades to Postgres");
    }
}



