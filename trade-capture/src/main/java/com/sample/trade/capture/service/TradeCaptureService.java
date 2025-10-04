package com.sample.trade.capture.service;

import com.sample.trade.common.model.Trade;
import org.springframework.beans.factory.annotation.Value;

public interface TradeCaptureService {

    @Value("${kafka.ingestion.topic.name}")
    public final String KAFKA_TOPIC_NAME = "";

    Trade readTradeMsgs(Trade trade);

    void persistTrade(Trade trade);

}
