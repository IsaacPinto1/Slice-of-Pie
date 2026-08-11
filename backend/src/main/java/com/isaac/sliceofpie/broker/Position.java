package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.instrument.Instrument;
import jakarta.persistence.*;

import java.math.BigDecimal;

// Near-exact copy of WatchlistItem, with no createdAt (a position
// references an external real-world holding - there's nothing meaningful
// about "when it was added" the way there is for a watchlist follow) and
// a quantity instead. See snaptrade-positions-spec.md.
@Entity
@Table(name = "position",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "instrument_id"}))
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    // Brokerage share counts can be fractional (e.g. Robinhood fractional
    // shares) - a stored quantity used in later math should never be a
    // double.
    @Column(nullable = false)
    private BigDecimal quantity;

    protected Position() {
        // required by JPA
    }

    public Position(Long userId, Instrument instrument, BigDecimal quantity) {
        this.userId = userId;
        this.instrument = instrument;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Instrument getInstrument() { return instrument; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
