ALTER TABLE watchlist ADD COLUMN instrument_id BIGINT;
ALTER TABLE watchlist ALTER COLUMN instrument_id SET NOT NULL;

ALTER TABLE watchlist ADD CONSTRAINT fk_watchlist_instrument
    FOREIGN KEY (instrument_id) REFERENCES instrument (id);
 
ALTER TABLE watchlist DROP CONSTRAINT IF EXISTS uq_watchlist_user_ticker;
ALTER TABLE watchlist DROP COLUMN ticker;
ALTER TABLE watchlist ADD CONSTRAINT uq_watchlist_user_instrument
    UNIQUE (user_id, instrument_id);