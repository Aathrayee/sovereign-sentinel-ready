package com.sovereign.sentinel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "heartbeat_record")
public class HeartbeatRecord {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SystemState status;

    @Column(nullable = false)
    private Instant lastPingAt;

    @Version
    private Long version;

    protected HeartbeatRecord() {
    }

    public HeartbeatRecord(Long id, SystemState status, Instant lastPingAt) {
        this.id = id;
        this.status = status;
        this.lastPingAt = lastPingAt;
    }

    public Long getId() {
        return id;
    }

    public SystemState getStatus() {
        return status;
    }

    public void setStatus(SystemState status) {
        this.status = status;
    }

    public Instant getLastPingAt() {
        return lastPingAt;
    }

    public void setLastPingAt(Instant lastPingAt) {
        this.lastPingAt = lastPingAt;
    }
}
