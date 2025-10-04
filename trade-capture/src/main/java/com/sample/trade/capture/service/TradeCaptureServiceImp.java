package com.sample.trade.capture.service;

import com.sample.trade.common.model.Trade;
import com.sample.trade.capture.model.TradeModel;
import com.sample.trade.validationstorage.service.TradeProjectionService;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TradeCaptureServiceImp implements TradeCaptureService {

    private final DynamoDBMapper dynamoDBMapper;
    private final AmazonDynamoDB amazonDynamoDB;

    @Autowired
    private TradeProjectionService tradeProjectionService;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    public TradeCaptureServiceImp(DynamoDBMapper dynamoDBMapper, AmazonDynamoDB amazonDynamoDB) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.amazonDynamoDB = amazonDynamoDB;
    }

    @Override
    public void persistTrade(Trade trade) {
        try {
            // Validate trade data
            if (trade == null) {
                throw new IllegalArgumentException("Trade cannot be null");
            }

            if (trade.getRequestId() == null || trade.getTradeId() == null) {
                throw new IllegalArgumentException("Trade requestId and tradeId cannot be null");
            }

            // Convert Trade to TradeModel for DynamoDB persistence
            TradeModel tradeModel = new TradeModel();

            // Set fields directly - using simple field assignment
            tradeModel.reqtradeid = trade.getRequestId() + "#" + trade.getTradeId() + "#" + trade.getVersion();
            tradeModel.tradeId = trade.getTradeId();
            tradeModel.version = trade.getVersion();
            tradeModel.counterPartyId = trade.getCounterPartyId();
            tradeModel.bookId = trade.getBookId();
            tradeModel.maturityDate = trade.getMaturityDate();
            tradeModel.createdDate = trade.getCreatedDate();
            tradeModel.expired = trade.getExpired();

            System.out.println("Attempting to save trade to DynamoDB:");
            System.out.println("  - reqtradeid: " + tradeModel.reqtradeid);
            System.out.println("  - tradeId: " + tradeModel.tradeId);
            System.out.println("  - version: " + tradeModel.version);
            System.out.println("  - tableName: " + tableName);

            // Try DynamoDBMapper first, fallback to low-level client
            Map<String, AttributeValue> item = new HashMap<>();
            String reqtradeid = trade.getRequestId() + "#" + trade.getTradeId() + "#" + trade.getVersion();
            item.put("reqtradeid", new AttributeValue().withS(reqtradeid));
            item.put("tradeId", new AttributeValue().withS(tradeModel.tradeId));
            item.put("version", new AttributeValue().withN(String.valueOf(tradeModel.version)));
            item.put("counterPartyId", new AttributeValue().withS(tradeModel.counterPartyId));
            item.put("bookId", new AttributeValue().withS(tradeModel.bookId));
            item.put("maturityDate", new AttributeValue().withS(tradeModel.maturityDate.toString()));
            item.put("createdDate", new AttributeValue().withS(tradeModel.createdDate.toString()));
            item.put("expired", new AttributeValue().withS(tradeModel.expired));

            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(tableName)
                    .withItem(item);

            amazonDynamoDB.putItem(putItemRequest);
            System.out.println("Successfully saved using low-level DynamoDB client");

            System.out.println("Successfully persisted trade: " + trade.getTradeId() +
                    " version: " + trade.getVersion() +
                    " to DynamoDB table: " + tableName);

            // Update trade projection in validation-storage service
            if (tradeProjectionService != null) {
                try {
                    tradeProjectionService.updateTradeProjectStore(trade);
                    System.out.println("Successfully updated trade projection for: " + trade.getTradeId());
                } catch (Exception e) {
                    System.err.println("Error updating trade projection: " + e.getMessage());
                    // Don't fail the entire operation if projection update fails
                }
            } else {
                System.out.println("TradeProjectionService not available - skipping projection update");
            }

        } catch (Exception e) {
            System.err.println("Error persisting trade to DynamoDB: " + e.getMessage());
            System.err.println("Trade details: " + trade);
            System.err.println("TradeModel details: uniqueIdentifier="
                    + (trade != null ? trade.getRequestId() + "#" + trade.getTradeId() + "#" + trade.getVersion()
                            : "null"));
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw new RuntimeException("Failed to persist trade to DynamoDB", e);
        }
    }

    @Override
    @KafkaListener(topics = "trade_ingestion")
    public Trade readTradeMsgs(Trade trade) {
        System.out.println("Received trade from Kafka: " + trade);

        try {
            persistTrade(trade);
            return trade;
        } catch (Exception e) {
            System.err.println("Error processing trade message: " + e.getMessage());
            throw new RuntimeException("Failed to process trade message", e);
        }
    }
}
