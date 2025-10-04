package com.sample.trade.common.store;

import com.sample.trade.common.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class TradeStore {
    @Autowired
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TradeStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get the latest version of a trade by trade ID
     */
    public Integer getLatestVersion(String tradeId) {
        String sql = "SELECT version FROM trade_projection WHERE trade_id = ? ORDER BY version DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, tradeId);
        } catch (Exception e) {
            return null; // No existing trade found
        }
    }

    /**
     * Insert or update a valid trade in the projection table
     */
    public void upsertTrade(Trade trade) {
        String upsertSql = """
                INSERT INTO trade_projection (trade_id, version, counter_party_id, book_id, maturity_date, created_date, expired)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (trade_id, version)
                DO UPDATE SET
                    counter_party_id = EXCLUDED.counter_party_id,
                    book_id = EXCLUDED.book_id,
                    maturity_date = EXCLUDED.maturity_date,
                    created_date = EXCLUDED.created_date,
                    expired = EXCLUDED.expired
                """;

        jdbcTemplate.update(upsertSql,
                trade.getTradeId(),
                trade.getVersion(),
                trade.getCounterPartyId(),
                trade.getBookId(),
                trade.getMaturityDate(),
                trade.getCreatedDate(),
                trade.getExpired());

        System.out.println("Successfully updated trade projection: " + trade.getTradeId());
    }

    /**
     * Insert a rejected trade into the exception table
     */
    public void insertTradeException(Trade trade, String exceptionReason) {
        String insertExceptionSql = """
                INSERT INTO trade_exception (trade_id, version, counter_party_id, book_id, maturity_date, created_date, expired, exception_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(insertExceptionSql,
                trade.getTradeId(),
                trade.getVersion(),
                trade.getCounterPartyId(),
                trade.getBookId(),
                trade.getMaturityDate(),
                trade.getCreatedDate(),
                trade.getExpired(),
                exceptionReason,
                LocalDate.now());

        System.out.println("Trade rejected and stored in exception table: " + trade.getTradeId());
    }

    /**
     * Get all trades from the projection table
     */
    public List<Trade> getAllTrades() {
        String sql = "SELECT trade_id, version, counter_party_id, book_id, maturity_date, created_date, expired FROM trade_projection";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Trade trade = new Trade();
            trade.setTradeId(rs.getString("trade_id"));
            trade.setVersion(rs.getInt("version"));
            trade.setCounterPartyId(rs.getString("counter_party_id"));
            trade.setBookId(rs.getString("book_id"));
            trade.setMaturityDate(rs.getDate("maturity_date").toLocalDate());
            trade.setCreatedDate(rs.getDate("created_date").toLocalDate());
            trade.setExpired(rs.getString("expired"));
            return trade;
        });
    }

    /**
     * Update the expired status of a trade
     */
    public void updateTradeExpiry(String tradeId, int version, String expired) {
        String sql = "UPDATE trade_projection SET expired = ? WHERE trade_id = ? AND version = ?";
        jdbcTemplate.update(sql, expired, tradeId, version);
        System.out.println("Updated trade expiry status: " + tradeId + " version " + version + " to " + expired);
    }

    /**
     * Get trades that need to be expired (maturity date < today and not already
     * expired)
     */
    public List<Trade> getTradesToExpire() {
        String sql = """
                SELECT trade_id, version, counter_party_id, book_id, maturity_date, created_date, expired
                FROM trade_projection
                WHERE maturity_date < ? AND expired = 'N'
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Trade trade = new Trade();
            trade.setTradeId(rs.getString("trade_id"));
            trade.setVersion(rs.getInt("version"));
            trade.setCounterPartyId(rs.getString("counter_party_id"));
            trade.setBookId(rs.getString("book_id"));
            trade.setMaturityDate(rs.getDate("maturity_date").toLocalDate());
            trade.setCreatedDate(rs.getDate("created_date").toLocalDate());
            trade.setExpired(rs.getString("expired"));
            return trade;
        }, LocalDate.now());
    }
}
