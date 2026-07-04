package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.instrument.Instrument;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "watchlist",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "instrument_id"}))
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public WatchlistItem() {}

    public WatchlistItem(Long userId, Instrument instrument) {
        this.userId = userId;
        this.instrument = instrument;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Instrument getInstrument() { return instrument; }
    public Instant getCreatedAt() { return createdAt; }
}