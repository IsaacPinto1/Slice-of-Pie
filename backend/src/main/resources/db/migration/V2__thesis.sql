CREATE TABLE thesis (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,

    content TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL, 
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_thesis_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_ticker
        UNIQUE (user_id, ticker)
);

CREATE INDEX idx_thesis_user_id ON thesis(user_id);
CREATE INDEX idx_thesis_ticker ON thesis(ticker);