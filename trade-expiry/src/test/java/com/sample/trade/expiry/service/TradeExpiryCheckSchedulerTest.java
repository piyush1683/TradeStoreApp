package com.sample.trade.expiry.service;

import com.sample.trade.common.model.Trade;
import com.sample.trade.common.store.TradeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeExpiryCheckSchedulerTest {

    @Mock
    private TradeStore tradeStore;

    private TradeExpiryCheckScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TradeExpiryCheckScheduler();
        // Use reflection to set the private field
        try {
            java.lang.reflect.Field field = TradeExpiryCheckScheduler.class.getDeclaredField("tradeStore");
            field.setAccessible(true);
            field.set(scheduler, tradeStore);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set tradeStore field", e);
        }
    }

    @Test
    void testCheckExpiry_WithExpiredTrades() {
        // Given
        Trade expiredTrade1 = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().minusDays(1), LocalDate.now().minusDays(10), "N");
        Trade expiredTrade2 = createSampleTrade("T2", 1, "CP-2", "B2",
                LocalDate.now().minusDays(2), LocalDate.now().minusDays(5), "N");
        Trade validTrade = createSampleTrade("T3", 1, "CP-3", "B3",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");

        List<Trade> tradesToExpire = Arrays.asList(expiredTrade1, expiredTrade2);
        when(tradeStore.getTradesToExpire()).thenReturn(tradesToExpire);

        // When
        scheduler.checkExpiry();

        // Then
        verify(tradeStore).getTradesToExpire();
        verify(tradeStore).updateTradeExpiry("T1", 1, "Y");
        verify(tradeStore).updateTradeExpiry("T2", 1, "Y");
        verify(tradeStore, times(2)).updateTradeExpiry(anyString(), eq(1), eq("Y"));
    }

    @Test
    void testCheckExpiry_WithNoExpiredTrades() {
        // Given
        when(tradeStore.getTradesToExpire()).thenReturn(Arrays.asList());

        // When
        scheduler.checkExpiry();

        // Then
        verify(tradeStore).getTradesToExpire();
        verify(tradeStore, never()).updateTradeExpiry(anyString(), anyInt(), anyString());
    }

    @Test
    void testCheckExpiry_WithMixedTrades() {
        // Given
        Trade expiredTrade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().minusDays(1), LocalDate.now().minusDays(10), "N");
        Trade futureTrade = createSampleTrade("T2", 1, "CP-2", "B2",
                LocalDate.now().plusDays(1), LocalDate.now(), "N");

        List<Trade> tradesToExpire = Arrays.asList(expiredTrade);
        when(tradeStore.getTradesToExpire()).thenReturn(tradesToExpire);

        // When
        scheduler.checkExpiry();

        // Then
        verify(tradeStore).getTradesToExpire();
        verify(tradeStore).updateTradeExpiry("T1", 1, "Y");
        verify(tradeStore, times(1)).updateTradeExpiry(anyString(), anyInt(), anyString());
    }

    @Test
    void testCheckExpiry_WithAlreadyExpiredTrades() {
        // Given
        Trade alreadyExpiredTrade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().minusDays(1), LocalDate.now().minusDays(10), "Y");

        // This trade won't be returned by getTradesToExpire since it's already expired
        when(tradeStore.getTradesToExpire()).thenReturn(Arrays.asList());

        // When
        scheduler.checkExpiry();

        // Then
        verify(tradeStore).getTradesToExpire();
        verify(tradeStore, never()).updateTradeExpiry(anyString(), anyInt(), anyString());
    }

    @Test
    void testCheckExpiry_WithMultipleVersions() {
        // Given
        Trade tradeV1 = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().minusDays(1), LocalDate.now().minusDays(10), "N");
        Trade tradeV2 = createSampleTrade("T1", 2, "CP-1", "B1",
                LocalDate.now().minusDays(1), LocalDate.now().minusDays(5), "N");

        List<Trade> tradesToExpire = Arrays.asList(tradeV1, tradeV2);
        when(tradeStore.getTradesToExpire()).thenReturn(tradesToExpire);

        // When
        scheduler.checkExpiry();

        // Then
        verify(tradeStore).getTradesToExpire();
        verify(tradeStore).updateTradeExpiry("T1", 1, "Y");
        verify(tradeStore).updateTradeExpiry("T1", 2, "Y");
        verify(tradeStore, times(2)).updateTradeExpiry(anyString(), anyInt(), anyString());
    }

    @Test
    void testCheckExpiry_WithException() {
        // Given
        when(tradeStore.getTradesToExpire()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        // The method should handle the exception gracefully and not throw
        try {
            scheduler.checkExpiry();
        } catch (Exception e) {
            fail("checkExpiry should not throw exception: " + e.getMessage());
        }
        verify(tradeStore).getTradesToExpire();
        verify(tradeStore, never()).updateTradeExpiry(anyString(), anyInt(), anyString());
    }

    private Trade createSampleTrade(String tradeId, int version, String counterPartyId, String bookId,
            LocalDate maturityDate, LocalDate createdDate, String expired) {
        Trade trade = new Trade();
        trade.setTradeId(tradeId);
        trade.setVersion(version);
        trade.setCounterPartyId(counterPartyId);
        trade.setBookId(bookId);
        trade.setMaturityDate(maturityDate);
        trade.setCreatedDate(createdDate);
        trade.setExpired(expired);
        return trade;
    }
}
