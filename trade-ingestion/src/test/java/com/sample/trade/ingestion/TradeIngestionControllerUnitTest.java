package com.sample.trade.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.trade.common.model.Trade;
import com.sample.trade.ingestion.api.TradeIngestionController;
import com.sample.trade.ingestion.service.KafkaTradeIngestionService;

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
        private KafkaTradeIngestionService kafkaTradeIngestionService;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                controller = new TradeIngestionController(kafkaTradeIngestionService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

        @Test
        void postSingleTrade_returnsAccepted_andPublishesToKafka() throws Exception {
                // Given
        }

}
