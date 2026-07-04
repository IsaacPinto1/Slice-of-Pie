package com.isaac.sliceofpie.thesis;

import com.isaac.sliceofpie.instrument.Instrument;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "thesis",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "instrument_id"}))
public class Thesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Thesis() {}

    public Thesis(Long userId, Instrument instrument, String content) {
        this.userId = userId;
        this.instrument = instrument;
        this.content = content;
    }

    @PrePersist
    public void onCreate() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Instrument getInstrument() { return instrument; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setContent(String content) { this.content = content; }
}