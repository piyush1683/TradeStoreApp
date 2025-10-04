package com.sample.trade.ingestion.service;

import com.sample.trade.common.model.Trade;
import com.sample.trade.ingestion.api.TradeIngestionException;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

@Service
@Primary
public class KafkaTradeIngestionService {

    private final KafkaTemplate<String, Trade> kafkaTemplate;

    @Value("${kafka.ingestion.topic.name}")
    private String topicName;

    public KafkaTradeIngestionService(KafkaTemplate<String, Trade> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTradetoKafka(Trade trade) {
        try {
            kafkaTemplate.send(topicName, trade.getTradeId(), trade).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            throw new TradeIngestionException("Failed to publish trade to Kafka", e);
        }
    }

}
