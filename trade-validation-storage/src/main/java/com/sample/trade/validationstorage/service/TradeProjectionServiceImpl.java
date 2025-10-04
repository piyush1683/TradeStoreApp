package com.sample.trade.validationstorage.service;

import com.sample.trade.common.model.Trade;
import com.sample.trade.common.store.TradeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TradeProjectionServiceImpl implements TradeProjectionService {

    private final TradeStore tradeStore;
    private Trade currentTrade;

    @Autowired
    public TradeProjectionServiceImpl(TradeStore tradeStore) {
        this.tradeStore = tradeStore;
    }

    @Override
    public Trade readTradeEventStore() {
        return null;
    }

    @Override
    public void updateTradeProjectStore(Trade trade) {
        if (trade == null) {
            System.out.println("No trade to update");
            return;
        }

        this.currentTrade = trade;

        try {
            // Validate trade first
            if (validateTrade()) {
                // Insert/Update in trade projection table (PostgreSQL)
                tradeStore.insertTrade(trade);
                System.out.println("Successfully updated trade projection store for trade: " + trade.getTradeId());
            } else {
                // Insert into trade exception table
                String exceptionReason = getValidationFailureReason();
                tradeStore.insertTradeException(trade, exceptionReason);
                System.out.println("Trade validation failed, stored in exception table: " + trade.getTradeId());
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

            return true;
        } catch (Exception e) {
            System.err.println("Error validating trade: " + e.getMessage());
            return false;
        }
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
