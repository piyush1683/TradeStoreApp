package com.sample.trade.ingestion.api;

public class TradeIngestionException extends RuntimeException {
    public TradeIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
