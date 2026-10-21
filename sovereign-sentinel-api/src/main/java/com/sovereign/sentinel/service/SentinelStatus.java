package com.sovereign.sentinel.service;

import java.time.Instant;

public record SentinelStatus(
        String status,
        Instant lastPing,
        Instant deadline,
        long secondsRemaining
) {
}
