package com.sample.trade.expiry.service;

import com.sample.trade.common.model.Trade;
import com.sample.trade.common.store.TradeStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sample.trade.common.store.TradeStore;

import java.time.LocalDate;
import java.util.List;

@Component
public class TradeExpiryCheckScheduler {
    private static final Logger logger = LogManager.getLogger(TradeExpiryCheckScheduler.class);

    @Autowired
    private TradeStore tradeStore;

    /**
     * This method is executed after every 5 seconds by the Spring Scheduler.
     * It iterates all the trade available in trade-store and sets expiry of those
     * trade which are older than current date/
     * 
     */

    @Scheduled(fixedRate = 5000)
    public void checkExpiry() {
        LocalDate today = LocalDate.now();

        // Get trades that need to be expired
        List<Trade> tradesToExpire = tradeStore.getTradesToExpire();

        for (Trade trade : tradesToExpire) {
            if (trade.getMaturityDate().isBefore(today)) {
                logger.info("Trade: " + trade.getTradeId() + " has been expired");

                // Update the trade expiry status in the database
                tradeStore.updateTradeExpiry(trade.getTradeId(), trade.getVersion(), "Y");
            }
        }
    }
}