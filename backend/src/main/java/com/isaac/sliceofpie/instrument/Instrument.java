package com.isaac.sliceofpie.instrument;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "instrument",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ticker"}))
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(nullable = false)
    private String name;

    // Nullable for now - Finnhub's /search endpoint doesn't return exchange.
    // Can be enriched later via /stock/profile2 if needed.
    private String exchange;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Instrument() {
        // required by JPA
    }

    public Instrument(String ticker, String name, String exchange) {
        this.ticker = ticker;
        this.name = name;
        this.exchange = exchange;
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public String getExchange() {
        return exchange;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}