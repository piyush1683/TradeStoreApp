package com.sample.trade.capture.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.trade.common.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeCaptureServiceTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private ObjectMapper objectMapper;

    private TradeCaptureServiceImp tradeCaptureService;

    @BeforeEach
    void setUp() {
        tradeCaptureService = new TradeCaptureServiceImp(dynamoDbClient, objectMapper);
    }

    @Test
    void testPersistTrade_Success() throws Exception {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");

        PutItemResponse response = PutItemResponse.builder().build();
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(response);

        // When
        tradeCaptureService.persistTrade(trade);

        // Then
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }

    @Test
    void testPersistTrade_WithException() {
        // Given
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(new RuntimeException("DynamoDB error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> tradeCaptureService.persistTrade(trade));
    }

    @Test
    void testReadTradeMsgs_Success() throws Exception {
        // Given
        String message = "{\"tradeId\":\"T1\",\"version\":1,\"counterPartyId\":\"CP-1\",\"bookId\":\"B1\",\"maturityDate\":\"2024-12-31\",\"createdDate\":\"2024-01-01\",\"expired\":\"N\"}";
        Trade expectedTrade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.of(2024, 12, 31), LocalDate.of(2024, 1, 1), "N");

        when(objectMapper.readValue(message, Trade.class)).thenReturn(expectedTrade);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // When
        Trade result = tradeCaptureService.readTradeMsgs(message);

        // Then
        assertEquals(expectedTrade, result);
        verify(objectMapper).readValue(message, Trade.class);
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }

    @Test
    void testReadTradeMsgs_WithJsonException() throws Exception {
        // Given
        String message = "invalid json";
        when(objectMapper.readValue(message, Trade.class))
                .thenThrow(new RuntimeException("JSON parsing error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> tradeCaptureService.readTradeMsgs(message));
    }

    @Test
    void testReadTradeMsgs_WithDynamoDBException() throws Exception {
        // Given
        String message = "{\"tradeId\":\"T1\"}";
        Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(5), LocalDate.now(), "N");

        when(objectMapper.readValue(message, Trade.class)).thenReturn(trade);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(new RuntimeException("DynamoDB error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> tradeCaptureService.readTradeMsgs(message));
    }

    @Test
    void testPersistTrade_VerifyDynamoDBItemStructure() throws Exception {
        // Given
        Trade trade = createSampleTrade("T2", 2, "CP-2", "B2",
                LocalDate.of(2024, 6, 15), LocalDate.of(2024, 1, 1), "N");

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // When
        tradeCaptureService.persistTrade(trade);

        // Then
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
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
