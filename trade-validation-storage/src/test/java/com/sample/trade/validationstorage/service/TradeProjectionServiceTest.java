package com.sample.trade.validationstorage.service;

import com.sample.trade.common.model.Trade;
import com.sample.trade.common.store.TradeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeProjectionServiceTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private TradeStore tradeStore;

    private TradeProjectionServiceImpl tradeProjectionService;

    @BeforeEach
    void setUp() {
        tradeProjectionService = new TradeProjectionServiceImpl(dynamoDbClient, tradeStore);
    }

    private void setCurrentTrade(Trade trade) {
        try {
            java.lang.reflect.Field field = TradeProjectionServiceImpl.class.getDeclaredField("currentTrade");
            field.setAccessible(true);
            field.set(tradeProjectionService, trade);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set currentTrade field", e);
        }
    }

    @Test
    void testReadTradeEventStore_Success() {
        // Given
        Map<String, AttributeValue> item = createDynamoDBItem("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");
        ScanResponse response = ScanResponse.builder()
                .items(Arrays.asList(item))
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(response);

        // When
        Trade result = tradeProjectionService.readTradeEventStore();

        // Then
        assertNotNull(result);
        assertEquals("T1", result.getTradeId());
        assertEquals(1, result.getVersion());
        assertEquals("CP-1", result.getCounterPartyId());
        assertEquals("B1", result.getBookId());
        assertEquals("N", result.getExpired());
    }

    @Test
    void testReadTradeEventStore_EmptyResult() {
        // Given
        ScanResponse response = ScanResponse.builder()
                .items(Arrays.asList())
                .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(response);

        // When
        Trade result = tradeProjectionService.readTradeEventStore();

        // Then
        assertNull(result);
    }

    @Test
    void testReadTradeEventStore_WithException() {
        // Given
        when(dynamoDbClient.scan(any(ScanRequest.class)))
                .thenThrow(new RuntimeException("DynamoDB error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> tradeProjectionService.readTradeEventStore());
    }

    @Test
    void testUpdateTradeProjectStore_ValidTrade() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");
        setCurrentTrade(trade);

        when(tradeStore.getLatestVersion("T1")).thenReturn(null);

        // When
        tradeProjectionService.updateTradeProjectStore();

        // Then
        verify(tradeStore).upsertTrade(trade);
        verify(tradeStore, never()).insertTradeException(any(Trade.class), anyString());
    }

    @Test
    void testUpdateTradeProjectStore_InvalidTrade_LowerVersion() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");
        setCurrentTrade(trade);

        when(tradeStore.getLatestVersion("T1")).thenReturn(2);

        // When
        tradeProjectionService.updateTradeProjectStore();

        // Then
        verify(tradeStore).insertTradeException(eq(trade), anyString());
        verify(tradeStore, never()).upsertTrade(any(Trade.class));
    }

    @Test
    void testUpdateTradeProjectStore_InvalidTrade_PastMaturity() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().minusDays(1), LocalDate.now(), "N");
        setCurrentTrade(trade);

        when(tradeStore.getLatestVersion("T1")).thenReturn(null);

        // When
        tradeProjectionService.updateTradeProjectStore();

        // Then
        verify(tradeStore).insertTradeException(eq(trade), anyString());
        verify(tradeStore, never()).upsertTrade(any(Trade.class));
    }

    @Test
    void testValidateTrade_ValidTrade() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");
        setCurrentTrade(trade);

        when(tradeStore.getLatestVersion("T1")).thenReturn(null);

        // When
        boolean result = tradeProjectionService.validateTrade();

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateTrade_LowerVersion() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");
        setCurrentTrade(trade);

        when(tradeStore.getLatestVersion("T1")).thenReturn(2);

        // When
        boolean result = tradeProjectionService.validateTrade();

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateTrade_PastMaturity() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().minusDays(1), LocalDate.now(), "N");
        setCurrentTrade(trade);

        when(tradeStore.getLatestVersion("T1")).thenReturn(null);

        // When
        boolean result = tradeProjectionService.validateTrade();

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateTrade_SameVersion() {
        // Given
        Trade trade = createSampleTrade("T1", 2, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");
        setCurrentTrade(trade);

        when(tradeStore.getLatestVersion("T1")).thenReturn(2);

        // When
        boolean result = tradeProjectionService.validateTrade();

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateTrade_NoCurrentTrade() {
        // Given
        setCurrentTrade(null);

        // When
        boolean result = tradeProjectionService.validateTrade();

        // Then
        assertFalse(result);
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

    private Map<String, AttributeValue> createDynamoDBItem(String tradeId, int version, String counterPartyId,
            String bookId, LocalDate maturityDate,
            LocalDate createdDate, String expired) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("tradeId", AttributeValue.builder().s(tradeId).build());
        item.put("version", AttributeValue.builder().n(String.valueOf(version)).build());
        item.put("counterPartyId", AttributeValue.builder().s(counterPartyId).build());
        item.put("bookId", AttributeValue.builder().s(bookId).build());
        item.put("maturityDate", AttributeValue.builder().s(maturityDate.toString()).build());
        item.put("createdDate", AttributeValue.builder().s(createdDate.toString()).build());
        item.put("expired", AttributeValue.builder().s(expired).build());
        return item;
    }
}
