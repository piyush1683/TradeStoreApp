package com.sample.trade.ingestion.api;

import com.sample.trade.common.model.Trade;

import java.util.List;

public interface TradeIngestionService {

    void acceptTrade(Trade trade);

    void acceptTrades(List<Trade> trades);
}
