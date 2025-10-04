package com.sample.trade.validationstorage.service;

import com.sample.trade.common.store.TradeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TradeExpiryCheckScheduler {

    @Autowired
    private TradeStore tradeStore;

    @Scheduled(fixedRate = 3000) // Run every 5 minutes
    public void checkExpiry() {
        try {
            System.out.println("Starting trade expiry check at: " + java.time.LocalDateTime.now());

            // Get all active trades
            List<String> activeTradeIds = tradeStore.getActiveTradeIds();

            if (activeTradeIds.isEmpty()) {
                System.out.println("No active trades found for expiry check");
                return;
            }

            int expiredCount = 0;
            LocalDate today = LocalDate.now();

            for (String tradeId : activeTradeIds) {
                try {
                    // Get trade details
                    var trade = tradeStore.getTradeById(tradeId);
                    if (trade != null && trade.getMaturityDate().isBefore(today)) {
                        // Mark trade as expired
                        tradeStore.markTradeAsExpired(tradeId);
                        expiredCount++;
                        System.out.println("Marked trade as expired: " + tradeId +
                                " (Maturity: " + trade.getMaturityDate() + ")");
                    }
                } catch (Exception e) {
                    System.err.println("Error checking expiry for trade " + tradeId + ": " + e.getMessage());
                }
            }

            System.out.println("Expiry check completed. Expired trades: " + expiredCount);

        } catch (Exception e) {
            System.err.println("Error during trade expiry check: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
