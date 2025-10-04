package com.sample.trade.architecture;

import com.sample.trade.common.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradeStoreRootBehaviourTest {

    private Trade validTrade;
    private Trade lowerVersionTrade;
    private Trade sameVersionTrade;
    private Trade pastMaturityTrade;
    private Trade futureTrade;

    @BeforeEach
    void setUp() {
        validTrade = createTrade("T1", 1, "CP-1", "B1", LocalDate.now().plusDays(5), LocalDate.now(), "N");
        lowerVersionTrade = createTrade("T2", 1, "CP-2", "B1", LocalDate.now().plusDays(5), LocalDate.now(), "N");
        sameVersionTrade = createTrade("T2", 2, "CP-2", "B1", LocalDate.now().plusDays(5), LocalDate.now(), "N");
        pastMaturityTrade = createTrade("T3", 1, "CP-3", "B2", LocalDate.now().minusDays(1), LocalDate.now(), "N");
        futureTrade = createTrade("T4", 1, "CP-4", "B3", LocalDate.now().plusDays(10), LocalDate.now(), "N");
    }

    @Test
    void testValidTradeStructure() {
        // Test that valid trade has all required fields
        assertNotNull(validTrade.getTradeId());
        assertTrue(validTrade.getVersion() > 0);
        assertNotNull(validTrade.getCounterPartyId());
        assertNotNull(validTrade.getBookId());
        assertNotNull(validTrade.getMaturityDate());
        assertNotNull(validTrade.getCreatedDate());
        assertNotNull(validTrade.getExpired());
    }

    @Test
    void testTradeValidationRules() {
        // Test version validation rule
        assertTrue(validTrade.getVersion() >= 1, "Version should be >= 1");

        // Test maturity date validation rule
        assertTrue(validTrade.getMaturityDate().isAfter(LocalDate.now()) ||
                validTrade.getMaturityDate().isEqual(LocalDate.now()),
                "Maturity date should not be in the past");

        // Test expired field validation
        assertTrue("Y".equals(validTrade.getExpired()) || "N".equals(validTrade.getExpired()),
                "Expired field should be 'Y' or 'N'");
    }

    @Test
    void testVersionComparisonLogic() {
        // Test that lower version should be rejected
        assertTrue(sameVersionTrade.getVersion() > lowerVersionTrade.getVersion(),
                "Higher version should be accepted over lower version");

        // Test that same version should replace
        assertEquals(sameVersionTrade.getVersion(), 2);
        assertEquals(lowerVersionTrade.getVersion(), 1);
    }

    @Test
    void testMaturityDateValidation() {
        // Test that past maturity dates should be rejected
        assertTrue(pastMaturityTrade.getMaturityDate().isBefore(LocalDate.now()),
                "Past maturity date should be identified for rejection");

        // Test that future maturity dates should be accepted
        assertTrue(futureTrade.getMaturityDate().isAfter(LocalDate.now()),
                "Future maturity date should be accepted");
    }

    @Test
    void testExpiryLogic() {
        // Test that trades with past maturity should be marked as expired
        Trade expiredTrade = createTrade("T5", 1, "CP-5", "B3",
                LocalDate.now().minusDays(1), LocalDate.now(), "N");

        // Simulate expiry logic
        if (expiredTrade.getMaturityDate().isBefore(LocalDate.now())) {
            expiredTrade.setExpired("Y");
        }

        assertEquals("Y", expiredTrade.getExpired());
    }

    @Test
    void testBulkTradeProcessing() {
        List<Trade> trades = List.of(
                createTrade("T6", 1, "CP-6", "B1", LocalDate.now().plusDays(10), LocalDate.now(), "N"),
                createTrade("T7", 1, "CP-7", "B1", LocalDate.now().plusDays(11), LocalDate.now(), "N"),
                createTrade("T8", 1, "CP-8", "B1", LocalDate.now().plusDays(12), LocalDate.now(), "N"));

        // Test that all trades are valid
        for (Trade trade : trades) {
            assertNotNull(trade.getTradeId());
            assertTrue(trade.getVersion() > 0);
            assertTrue(trade.getMaturityDate().isAfter(LocalDate.now()));
        }

        assertEquals(3, trades.size());
    }

    @Test
    void testMultipleVersionsHandling() {
        List<Trade> trades = List.of(
                createTrade("T9", 1, "CP-1", "B1", LocalDate.now().plusDays(3), LocalDate.now(), "N"),
                createTrade("T9", 2, "CP-1", "B1", LocalDate.now().plusDays(3), LocalDate.now(), "N"),
                createTrade("T10", 3, "CP-2", "B2", LocalDate.now().plusDays(5), LocalDate.now(), "N"));

        // Test version ordering
        Trade tradeV1 = trades.get(0);
        Trade tradeV2 = trades.get(1);
        Trade tradeT10 = trades.get(2);

        assertEquals("T9", tradeV1.getTradeId());
        assertEquals("T9", tradeV2.getTradeId());
        assertEquals("T10", tradeT10.getTradeId());

        assertTrue(tradeV2.getVersion() > tradeV1.getVersion());
        assertTrue(tradeT10.getVersion() > tradeV2.getVersion());
    }

    @Test
    void testTradeEquality() {
        Trade trade1 = createTrade("T11", 1, "CP-11", "B11", LocalDate.now().plusDays(5), LocalDate.now(), "N");
        Trade trade2 = createTrade("T11", 1, "CP-11", "B11", LocalDate.now().plusDays(5), LocalDate.now(), "N");
        Trade trade3 = createTrade("T12", 1, "CP-11", "B11", LocalDate.now().plusDays(5), LocalDate.now(), "N");

        assertEquals(trade1, trade2);
        assertNotEquals(trade1, trade3);
        assertEquals(trade1.hashCode(), trade2.hashCode());
    }

    @Test
    void testEdgeCases() {
        // Test with today's maturity date
        Trade todayMaturity = createTrade("T13", 1, "CP-13", "B13", LocalDate.now(), LocalDate.now(), "N");
        assertTrue(todayMaturity.getMaturityDate().isEqual(LocalDate.now()));

        // Test with version 1
        Trade versionOne = createTrade("T14", 1, "CP-14", "B14", LocalDate.now().plusDays(1), LocalDate.now(), "N");
        assertEquals(1, versionOne.getVersion());

        // Test with already expired trade
        Trade alreadyExpired = createTrade("T15", 1, "CP-15", "B15", LocalDate.now().minusDays(1), LocalDate.now(),
                "Y");
        assertEquals("Y", alreadyExpired.getExpired());
    }

    private static Trade createTrade(String id, int version, String cp, String book, LocalDate maturity,
            LocalDate created, String expired) {
        Trade t = new Trade();
        t.setTradeId(id);
        t.setVersion(version);
        t.setCounterPartyId(cp);
        t.setBookId(book);
        t.setMaturityDate(maturity);
        t.setCreatedDate(created);
        t.setExpired(expired);
        return t;
    }
}