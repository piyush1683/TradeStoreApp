package com.sample.trade.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.trade.common.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeIngestionControllerUnitTest {

    @Mock
    private KafkaTemplate<String, Trade> kafkaTemplate;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TradeIngestionController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        controller = new TradeIngestionController(kafkaTemplate, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postSingleTrade_returnsAccepted_andPublishesToKafka() throws Exception {
        // Given
        Trade sampleTrade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(1), LocalDate.now(), "N");

        CompletableFuture<SendResult<String, Trade>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(eq("trade_ingestion"), eq("T1"), any(Trade.class)))
                .thenReturn(future);

        // When & Then
        mockMvc.perform(post("/api/trade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTrade)))
                .andExpect(status().isAccepted());

        verify(kafkaTemplate, times(1)).send(eq("trade_ingestion"), eq("T1"), any(Trade.class));
    }

    @Test
    void postTradesUpload_returnsAccepted_andPublishesEachLine() throws Exception {
        // Given
        String csv = "T1,1,CP-1,B1,01/01/2099,<today date>,N\n" +
                "T2,1,CP-2,B2,02/01/2099,01/01/2020,N\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trades.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        CompletableFuture<SendResult<String, Trade>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(Trade.class)))
                .thenReturn(future);

        // When & Then
        mockMvc.perform(multipart("/api/trades/upload").file(file))
                .andExpect(status().isAccepted());

        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any(Trade.class));
    }

    @Test
    void whenKafkaFails_returnsBadGateway() throws Exception {
        // Given
        Trade sampleTrade = createSampleTrade("T1", 1, "CP-1", "B1",
                LocalDate.now().plusDays(1), LocalDate.now(), "N");

        CompletableFuture<SendResult<String, Trade>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(eq("trade_ingestion"), eq("T1"), any(Trade.class)))
                .thenReturn(future);

        // When & Then
        mockMvc.perform(post("/api/trade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTrade)))
                .andExpect(status().isBadGateway());
    }

    @Test
    void postSingleTrade_withInvalidData_returnsBadRequest() throws Exception {
        // Given
        Trade invalidTrade = new Trade();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/trade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTrade)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTradesUpload_withEmptyFile_returnsBadRequest() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                "".getBytes(StandardCharsets.UTF_8));

        // When & Then
        mockMvc.perform(multipart("/api/trades/upload").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTradesUpload_withInvalidCsvFormat_returnsBadRequest() throws Exception {
        // Given
        String invalidCsv = "T1,1,CP-1,B1,invalid-date,<today date>,N\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.csv",
                "text/csv",
                invalidCsv.getBytes(StandardCharsets.UTF_8));

        // When & Then
        mockMvc.perform(multipart("/api/trades/upload").file(file))
                .andExpect(status().isBadRequest());
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
