package com.sample.trade.validationstorage.service;

import com.sample.trade.common.model.Trade;
import com.sample.trade.common.store.TradeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.util.Map;

@Service
public class TradeProjectionServiceImpl implements TradeProjectionService {

    private final DynamoDbClient dynamoDbClient;
    private final TradeStore tradeStore;
    private final String tableName = "trades";
    private Trade currentTrade;

    @Autowired
    public TradeProjectionServiceImpl(DynamoDbClient dynamoDbClient, TradeStore tradeStore) {
        this.dynamoDbClient = dynamoDbClient;
        this.tradeStore = tradeStore;
    }

    @Override
    public Trade readTradeEventStore() {
        try {
            // Scan DynamoDB table for new trades (in real implementation, use DynamoDB
            // Streams)
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            if (!response.items().isEmpty()) {
                Map<String, AttributeValue> item = response.items().get(0); // Get latest trade
                currentTrade = mapToTrade(item);
                System.out.println("Read trade from DynamoDB: " + currentTrade.getTradeId());
                return currentTrade;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error reading from DynamoDB: " + e.getMessage());
            throw new RuntimeException("Failed to read trade from event store", e);
        }
    }

    @Override
    public void updateTradeProjectStore() {
        if (currentTrade == null) {
            System.out.println("No trade to update");
            return;
        }

        try {
            // Validate trade first
            if (validateTrade()) {
                // Insert/Update in trade projection table (PostgreSQL)
                tradeStore.upsertTrade(currentTrade);
            } else {
                // Insert into trade exception table
                String exceptionReason = getValidationFailureReason();
                tradeStore.insertTradeException(currentTrade, exceptionReason);
            }
        } catch (Exception e) {
            System.err.println("Error updating trade projection store: " + e.getMessage());
            throw new RuntimeException("Failed to update trade projection store", e);
        }
    }

    @Override
    public boolean validateTrade() {
        if (currentTrade == null) {
            return false;
        }

        try {
            // Rule 1: Check version - reject lower version, allow same version
            Integer latestVersion = tradeStore.getLatestVersion(currentTrade.getTradeId());

            if (latestVersion != null && currentTrade.getVersion() < latestVersion) {
                System.out.println("Rejecting trade: Lower version received. Current: " + currentTrade.getVersion()
                        + ", Latest: " + latestVersion);
                return false;
            }

            // Rule 2: Check maturity date - reject if earlier than today
            if (currentTrade.getMaturityDate().isBefore(LocalDate.now())) {
                System.out.println(
                        "Rejecting trade: Maturity date is in the past. Maturity: " + currentTrade.getMaturityDate());
                return false;
            }

            // Rule 3: Auto-expire trades with surpassed maturity date
            if (currentTrade.getMaturityDate().isBefore(LocalDate.now())) {
                currentTrade.setExpired("Y");
                System.out.println("Auto-expiring trade: " + currentTrade.getTradeId());
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error validating trade: " + e.getMessage());
            return false;
        }
    }

    private Trade mapToTrade(Map<String, AttributeValue> item) {
        Trade trade = new Trade();
        trade.setTradeId(item.get("tradeId").s());
        trade.setVersion(Integer.parseInt(item.get("version").n()));
        trade.setCounterPartyId(item.get("counterPartyId").s());
        trade.setBookId(item.get("bookId").s());
        trade.setMaturityDate(LocalDate.parse(item.get("maturityDate").s()));
        trade.setCreatedDate(LocalDate.parse(item.get("createdDate").s()));
        trade.setExpired(item.get("expired").s());
        return trade;
    }

    private String getValidationFailureReason() {
        if (currentTrade == null)
            return "No trade data";

        try {
            Integer latestVersion = tradeStore.getLatestVersion(currentTrade.getTradeId());

            if (latestVersion != null && currentTrade.getVersion() < latestVersion) {
                return "Lower version received: " + currentTrade.getVersion() + " < " + latestVersion;
            }

            if (currentTrade.getMaturityDate().isBefore(LocalDate.now())) {
                return "Maturity date in past: " + currentTrade.getMaturityDate();
            }

            return "Unknown validation failure";
        } catch (Exception e) {
            return "Validation error: " + e.getMessage();
        }
    }
}
