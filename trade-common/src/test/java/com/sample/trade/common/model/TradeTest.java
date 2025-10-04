package com.sample.trade.common.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TradeTest {

    @Test
    void testDefaultConstructor() {
        Trade trade = new Trade();
        assertNotNull(trade);
        assertNull(trade.getTradeId());
        assertEquals(0, trade.getVersion());
        assertNull(trade.getCounterPartyId());
        assertNull(trade.getBookId());
        assertNull(trade.getMaturityDate());
        assertNull(trade.getCreatedDate());
        assertNull(trade.getExpired());
    }

    @Test
    void testParameterizedConstructor() {
        LocalDate maturityDate = LocalDate.now().plusDays(10);
        LocalDate createdDate = LocalDate.now();

        Trade trade = new Trade("T1", 1, "CP-1", "B1", maturityDate, createdDate, "N", "requestId1");

        assertEquals("T1", trade.getTradeId());
        assertEquals(1, trade.getVersion());
        assertEquals("CP-1", trade.getCounterPartyId());
        assertEquals("B1", trade.getBookId());
        assertEquals(maturityDate, trade.getMaturityDate());
        assertEquals(createdDate, trade.getCreatedDate());
        assertEquals("N", trade.getExpired());
    }

    @Test
    void testGettersAndSetters() {
        Trade trade = new Trade();
        LocalDate maturityDate = LocalDate.now().plusDays(5);
        LocalDate createdDate = LocalDate.now();

        trade.setTradeId("T2");
        trade.setVersion(2);
        trade.setCounterPartyId("CP-2");
        trade.setBookId("B2");
        trade.setMaturityDate(maturityDate);
        trade.setCreatedDate(createdDate);
        trade.setExpired("Y");

        assertEquals("T2", trade.getTradeId());
        assertEquals(2, trade.getVersion());
        assertEquals("CP-2", trade.getCounterPartyId());
        assertEquals("B2", trade.getBookId());
        assertEquals(maturityDate, trade.getMaturityDate());
        assertEquals(createdDate, trade.getCreatedDate());
        assertEquals("Y", trade.getExpired());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDate maturityDate = LocalDate.now().plusDays(3);
        LocalDate createdDate = LocalDate.now();

        Trade trade1 = new Trade("T3", 1, "CP-3", "B3", maturityDate, createdDate, "N", "requestId1");
        Trade trade2 = new Trade("T3", 1, "CP-3", "B3", maturityDate, createdDate, "N", "requestId1");
        Trade trade3 = new Trade("T4", 1, "CP-3", "B3", maturityDate, createdDate, "N", "requestId1");

        assertEquals(trade1, trade2);
        assertNotEquals(trade1, trade3);
        assertEquals(trade1.hashCode(), trade2.hashCode());
        // Note: hashCode might be same for different objects, that's acceptable
    }

    @Test
    void testEqualsWithNull() {
        Trade trade = new Trade("T5", 1, "CP-5", "B5", LocalDate.now(), LocalDate.now(), "N", "requestId1");
        assertNotEquals(trade, null);
        assertNotEquals(trade, "not a trade");
    }

    @Test
    void testEqualsWithSelf() {
        Trade trade = new Trade("T6", 1, "CP-6", "B6", LocalDate.now(), LocalDate.now(), "N", "requestId1");
        assertEquals(trade, trade);
    }
}
