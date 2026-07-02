package com.isaac.sliceofpie.thesis;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "thesis",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}))
public class Thesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Thesis() {}

    public Thesis(Long userId, String ticker, String content) {
        this.userId = userId;
        this.ticker = ticker;
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
    public String getTicker() { return ticker; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setContent(String content) { this.content = content; }
}