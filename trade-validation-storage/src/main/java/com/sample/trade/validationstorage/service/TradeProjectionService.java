package com.sample.trade.validationstorage.service;

import com.sample.trade.common.model.Trade;

public interface TradeProjectionService {

    public Trade readTradeEventStore();

    public void updateTradeProjectStore(Trade tradeModel);

    public boolean validateTrade();
}
