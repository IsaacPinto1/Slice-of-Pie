CREATE TABLE instrument (
    id          BIGSERIAL PRIMARY KEY,
    ticker      VARCHAR(20)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    exchange    VARCHAR(50),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_instrument_ticker UNIQUE (ticker)
);

CREATE INDEX idx_instrument_ticker ON instrument (ticker);