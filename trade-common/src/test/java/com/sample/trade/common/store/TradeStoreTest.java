package com.sample.trade.common.store;

import com.sample.trade.common.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TradeStore tradeStore;

    @BeforeEach
    void setUp() {
        tradeStore = new TradeStore(jdbcTemplate);
    }

    @Test
    void testGetLatestVersion_WhenTradeExists() {
        // Given
        String tradeId = "T1";
        Integer expectedVersion = 3;
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(tradeId)))
                .thenReturn(expectedVersion);

        // When
        Integer result = tradeStore.getLatestVersion(tradeId);

        // Then
        assertEquals(expectedVersion, result);
        verify(jdbcTemplate).queryForObject(
                "SELECT version FROM trade_projection WHERE trade_id = ? ORDER BY version DESC LIMIT 1",
                Integer.class, tradeId);
    }

    @Test
    void testGetLatestVersion_WhenTradeDoesNotExist() {
        // Given
        String tradeId = "T1";
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(tradeId)))
                .thenThrow(new RuntimeException("No data"));

        // When
        Integer result = tradeStore.getLatestVersion(tradeId);

        // Then
        assertNull(result);
    }

    @Test
    void testUpsertTrade() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1", LocalDate.now().plusDays(5), LocalDate.now(), "N");
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        // When
        tradeStore.insertTrade(trade);

        // Then
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void testInsertTradeException() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1", LocalDate.now().plusDays(5), LocalDate.now(), "N");
        String exceptionReason = "Lower version received";
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        // When
        tradeStore.insertTradeException(trade, exceptionReason);

        // Then
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void testUpdateTradeExpiry() {
        // Given
        String tradeId = "T1";
        int version = 1;
        String expired = "Y";
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        // When
        tradeStore.updateTradeExpiry(tradeId, version, expired);

        // Then
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
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