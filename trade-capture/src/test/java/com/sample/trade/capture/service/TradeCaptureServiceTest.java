package com.sample.trade.capture.service;

import com.sample.trade.common.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.sample.trade.capture.model.TradeModel;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeCaptureServiceTest {

        @Mock
        private DynamoDBMapper dynamoDBMapper;

        @Mock
        private AmazonDynamoDB amazonDynamoDB;

        private TradeCaptureServiceImp tradeCaptureService;

        @BeforeEach
        void setUp() {
                tradeCaptureService = new TradeCaptureServiceImp(dynamoDBMapper, amazonDynamoDB);
        }

        @Test
        void testPersistTrade_Success() {
                // Given
                Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                                LocalDate.now().plusDays(5), LocalDate.now(), "N");

                // When
                tradeCaptureService.persistTrade(trade);

                // Then
                verify(dynamoDBMapper).save(any(TradeModel.class));
        }

        @Test
        void testPersistTrade_WithException() {
                // Given
                Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                                LocalDate.now().plusDays(5), LocalDate.now(), "N");

                doThrow(new RuntimeException("DynamoDB error"))
                                .when(dynamoDBMapper).save(any(TradeModel.class));

                // When & Then
                assertThrows(RuntimeException.class, () -> tradeCaptureService.persistTrade(trade));
        }

        @Test
        void testReadTradeMsgs_Success() {
                // Given
                Trade expectedTrade = createSampleTrade("T1", 1, "CP-1", "B1",
                                LocalDate.of(2024, 12, 31), LocalDate.of(2024, 1, 1), "N");

                // When
                Trade result = tradeCaptureService.readTradeMsgs(expectedTrade);

                // Then
                assertEquals(expectedTrade, result);
                verify(dynamoDBMapper).save(any(TradeModel.class));
        }

        @Test
        void testReadTradeMsgs_WithDynamoDBException() {
                // Given
                Trade trade = createSampleTrade("T1", 1, "CP-1", "B1",
                                LocalDate.now().plusDays(5), LocalDate.now(), "N");

                doThrow(new RuntimeException("DynamoDB error"))
                                .when(dynamoDBMapper).save(any(TradeModel.class));

                // When & Then
                assertThrows(RuntimeException.class, () -> tradeCaptureService.readTradeMsgs(trade));
        }

        @Test
        void testPersistTrade_VerifyDynamoDBItemStructure() {
                // Given
                Trade trade = createSampleTrade("T2", 2, "CP-2", "B2",
                                LocalDate.of(2024, 6, 15), LocalDate.of(2024, 1, 1), "N");

                // When
                tradeCaptureService.persistTrade(trade);

                // Then
                verify(dynamoDBMapper).save(any(TradeModel.class));
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
