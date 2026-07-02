package com.isaac.sliceofpie.watchlist;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "watchlist",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}))
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public WatchlistItem() {}

    public WatchlistItem(Long userId, String ticker) {
        this.userId = userId;
        this.ticker = ticker;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTicker() { return ticker; }
    public Instant getCreatedAt() { return createdAt; }
}