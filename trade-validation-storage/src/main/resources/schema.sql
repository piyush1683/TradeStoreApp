-- Trade Projection Table (PostgreSQL)
CREATE TABLE IF NOT EXISTS trade_projection (
    trade_id VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    counter_party_id VARCHAR(50) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    maturity_date DATE NOT NULL,
    created_date DATE NOT NULL,
    expired VARCHAR(1) NOT NULL DEFAULT 'N',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trade_id, version)
);

-- Trade Exception Table (PostgreSQL)
CREATE TABLE IF NOT EXISTS trade_exception (
    id SERIAL PRIMARY KEY,
    trade_id VARCHAR(50) NOT NULL,
    request_id VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    counter_party_id VARCHAR(50) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    maturity_date DATE NOT NULL,
    created_date DATE NOT NULL,
    expired VARCHAR(1) NOT NULL,
    exception_reason TEXT NOT NULL,
    created_at DATE NOT NULL
);
