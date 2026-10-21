package com.sovereign.sentinel.service;

import com.sovereign.sentinel.model.HeartbeatRecord;
import com.sovereign.sentinel.model.SystemState;
import com.sovereign.sentinel.repository.HeartbeatRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeartbeatService implements ApplicationRunner {
    private static final long SINGLETON_ID = 1L;

    private final HeartbeatRepository repository;
    private final Clock clock;
    private final Duration heartbeatWindow;

    public HeartbeatService(HeartbeatRepository repository, Clock clock,
                            @Value("${sentinel.heartbeat.hours:24}") long heartbeatHours) {
        this.repository = repository;
        this.clock = clock;
        this.heartbeatWindow = Duration.ofHours(heartbeatHours);
    }

    // Creates the record once. Subsequent restarts reuse the H2 record if the file survived.
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!repository.existsById(SINGLETON_ID)) {
            repository.save(new HeartbeatRecord(SINGLETON_ID, SystemState.ACTIVE, clock.instant()));
        }
    }

    @Transactional
    public SentinelStatus ping() {
        HeartbeatRecord record = getRecordWithWriteLock();
        record.setLastPingAt(clock.instant());
        record.setStatus(SystemState.ACTIVE);
        return toStatus(record, clock.instant());
    }

    @Transactional
    public SentinelStatus getStatus() {
        HeartbeatRecord record = getRecordWithWriteLock();
        Instant now = clock.instant();
        expireIfNecessary(record, now);
        return toStatus(record, now);
    }

    // Returns true only when this invocation actually changes the state.
    @Transactional
    public boolean checkAndLockIfExpired() {
        HeartbeatRecord record = getRecordWithWriteLock();
        return expireIfNecessary(record, clock.instant());
    }

    private HeartbeatRecord getRecordWithWriteLock() {
        return repository.findLocked(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Heartbeat record not initialized"));
    }

    private boolean expireIfNecessary(HeartbeatRecord record, Instant now) {
        Instant deadline = record.getLastPingAt().plus(heartbeatWindow);
        if (!now.isBefore(deadline) && record.getStatus() != SystemState.LOCKDOWN) {
            record.setStatus(SystemState.LOCKDOWN);
            return true;
        }
        return false;
    }

    private SentinelStatus toStatus(HeartbeatRecord record, Instant now) {
        Instant deadline = record.getLastPingAt().plus(heartbeatWindow);
        long remaining = record.getStatus() == SystemState.LOCKDOWN
                ? 0L : Math.max(0, Duration.between(now, deadline).toSeconds());
        return new SentinelStatus(record.getStatus().name(), record.getLastPingAt(), deadline, remaining);
    }
}
