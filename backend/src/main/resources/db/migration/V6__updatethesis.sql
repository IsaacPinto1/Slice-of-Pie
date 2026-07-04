ALTER TABLE thesis ADD COLUMN instrument_id BIGINT;
ALTER TABLE thesis ALTER COLUMN instrument_id SET NOT NULL;

ALTER TABLE thesis ADD CONSTRAINT fk_thesis_instrument
    FOREIGN KEY (instrument_id) REFERENCES instrument (id);

ALTER TABLE thesis DROP CONSTRAINT IF EXISTS uq_user_ticker;
ALTER TABLE thesis DROP COLUMN ticker;
ALTER TABLE thesis ADD CONSTRAINT uq_thesis_user_instrument
    UNIQUE (user_id, instrument_id);