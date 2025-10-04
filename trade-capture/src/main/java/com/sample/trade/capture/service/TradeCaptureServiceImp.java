package com.sample.trade.capture.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.trade.common.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class TradeCaptureServiceImp implements TradeCaptureService {

    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;
    private final String tableName = "trades";

    @Autowired
    public TradeCaptureServiceImp(DynamoDbClient dynamoDbClient, ObjectMapper objectMapper) {
        this.dynamoDbClient = dynamoDbClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void persistTrade(Trade trade) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("tradeId", AttributeValue.builder().s(trade.getTradeId()).build());
            item.put("version", AttributeValue.builder().n(String.valueOf(trade.getVersion())).build());
            item.put("counterPartyId", AttributeValue.builder().s(trade.getCounterPartyId()).build());
            item.put("bookId", AttributeValue.builder().s(trade.getBookId()).build());
            item.put("maturityDate", AttributeValue.builder().s(trade.getMaturityDate().toString()).build());
            item.put("createdDate", AttributeValue.builder().s(trade.getCreatedDate().toString()).build());
            item.put("expired", AttributeValue.builder().s(trade.getExpired()).build());

            // Use composite key: tradeId + version for unique identification
            String compositeKey = trade.getTradeId() + "#" + trade.getVersion();
            item.put("compositeKey", AttributeValue.builder().s(compositeKey).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(putItemRequest);
            System.out
                    .println("Successfully persisted trade: " + trade.getTradeId() + " version: " + trade.getVersion());

        } catch (Exception e) {
            System.err.println("Error persisting trade to DynamoDB: " + e.getMessage());
            throw new RuntimeException("Failed to persist trade to DynamoDB", e);
        }
    }

    @Override
    @KafkaListener(topics = TradeCaptureService.KAFKA_TOPIC_NAME)
    public Trade readTradeMsgs(String message) {
        System.out.println("Received message from Kafka: " + message);

        try {
            Trade trade = objectMapper.readValue(message, Trade.class);
            persistTrade(trade);
            return trade;
        } catch (Exception e) {
            System.err.println("Error processing trade message: " + e.getMessage());
            throw new RuntimeException("Failed to process trade message", e);
        }
    }
}
