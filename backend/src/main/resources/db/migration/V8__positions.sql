CREATE TABLE position (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    instrument_id  BIGINT NOT NULL,
    quantity       NUMERIC NOT NULL,
    CONSTRAINT fk_position_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_position_instrument
        FOREIGN KEY (instrument_id)
        REFERENCES instrument(id),
    CONSTRAINT uq_position_user_instrument
        UNIQUE (user_id, instrument_id)
);

CREATE INDEX idx_position_user_id ON position(user_id);
