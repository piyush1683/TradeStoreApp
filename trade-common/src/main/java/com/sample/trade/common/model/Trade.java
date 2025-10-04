package com.sample.trade.common.model;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

public class Trade implements Comparable<Trade> {
    private String tradeId;
    private int version;
    private String counterPartyId;
    private String bookId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
    private LocalDate maturityDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
    private LocalDate createdDate;
    private String expired;
    private String requestId;

    public Trade() {
    }

    public Trade(String tradeId, int version, String counterPartyId, String bookId,
            LocalDate maturityDate, LocalDate createdDate, String expired, String requestId) {
        this.tradeId = tradeId;
        this.version = version;
        this.counterPartyId = counterPartyId;
        this.bookId = bookId;
        this.maturityDate = maturityDate;
        this.createdDate = createdDate;
        this.expired = expired;
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public void setCounterPartyId(String counterPartyId) {
        this.counterPartyId = counterPartyId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public String getExpired() {
        return expired;
    }

    public void setExpired(String expired) {
        this.expired = expired;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Trade)) {
            return false;
        }

        Trade t1 = (Trade) obj;

        if (this.tradeId.equals(t1.getTradeId()) &&
                this.version == t1.getVersion() &&
                this.bookId.equals(t1.getBookId()) &&
                this.counterPartyId.equals(t1.getCounterPartyId()) &&
                this.maturityDate.equals(t1.getMaturityDate()) &&
                this.createdDate.equals(t1.getCreatedDate()) &&
                this.expired.equals(t1.getExpired())) {

            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Trade [tradeId=" + tradeId + ", version=" + version + ", counterPartyId=" + counterPartyId + ", bookId="
                + bookId + ", maturityDate=" + maturityDate + ", createdDate=" + createdDate + ", expired=" + expired
                + "]";
    }

    @Override
    public int hashCode() {

        int hash = 7;

        hash = 19 * this.tradeId.hashCode();
        hash = 19 * Integer.hashCode(this.version);
        hash = 19 * this.bookId.hashCode();
        hash = 19 * this.counterPartyId.hashCode();
        hash = 19 * this.maturityDate.hashCode();
        hash = 19 * this.createdDate.hashCode();
        hash = 19 * this.expired.hashCode();

        return hash;

    }

    @Override
    public int compareTo(Trade other) {
        int idCompare = this.tradeId.compareTo(other.tradeId);
        if (idCompare != 0) {
            return idCompare;
        }
        return Integer.compare(this.version, other.version);
    }
}
