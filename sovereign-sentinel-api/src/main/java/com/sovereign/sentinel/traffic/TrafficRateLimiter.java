
package com.sovereign.sentinel.traffic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrafficRateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> windows =
            new ConcurrentHashMap<>();

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong blockedRequests = new AtomicLong();

    private final int maxRequests;
    private final long windowNanos;

    public TrafficRateLimiter(
            @Value("${sentinel.traffic.max-requests:30}") int maxRequests,
            @Value("${sentinel.traffic.window-seconds:60}") long windowSeconds) {

        if (maxRequests < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException(
                    "Traffic limits must be positive");
        }

        this.maxRequests = maxRequests;
        this.windowNanos = TimeUnit.SECONDS.toNanos(windowSeconds);
    }

    public Decision check(String clientAddress) {
        totalRequests.incrementAndGet();

        long now = System.nanoTime();
        AtomicReference<Decision> answer = new AtomicReference<>();

        // Atomic update for each client address.
        windows.compute(clientAddress, (address, timestamps) -> {

            if (timestamps == null) {
                timestamps = new ArrayDeque<>();
            }

            discardExpired(timestamps, now);

            if (timestamps.size() >= maxRequests) {
                blockedRequests.incrementAndGet();

                long waitNanos =
                        windowNanos - (now - timestamps.peekFirst());

                long retrySeconds = Math.max(
                        1,
                        (waitNanos + 999_999_999L) / 1_000_000_000L
                );

                answer.set(new Decision(false, retrySeconds));

            } else {
                timestamps.addLast(now);
                answer.set(new Decision(true, 0));
            }

            return timestamps;
        });

        return answer.get();
    }

    public Map<String, Long> statistics() {
        return Map.of(
                "totalRequests", totalRequests.get(),
                "blockedRequests", blockedRequests.get(),
                "trackedClients", (long) windows.size(),
                "maxRequests", (long) maxRequests,
                "windowSeconds",
                TimeUnit.NANOSECONDS.toSeconds(windowNanos)
        );
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredClients() {
        long now = System.nanoTime();

        windows.forEach((address, ignored) ->
                windows.computeIfPresent(address, (key, timestamps) -> {
                    discardExpired(timestamps, now);
                    return timestamps.isEmpty() ? null : timestamps;
                })
        );
    }

    private void discardExpired(Deque<Long> timestamps, long now) {
        while (!timestamps.isEmpty()
                && now - timestamps.peekFirst() >= windowNanos) {
            timestamps.removeFirst();
        }
    }

    public record Decision(
            boolean allowed,
            long retryAfterSeconds
    ) { }
}
