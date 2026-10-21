
package com.sovereign.sentinel.controller;

import com.sovereign.sentinel.security.KeyGuard;
import com.sovereign.sentinel.service.HeartbeatService;
import com.sovereign.sentinel.service.SentinelStatus;
import com.sovereign.sentinel.traffic.TrafficRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SentinelController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String[] BOT_HINTS = {
            "gptbot", "chatgpt-user", "claudebot",
            "anthropic-ai", "google-extended",
            "ccbot", "bytespider", "scrapy",
            "headlesschrome", "python-requests",
            "curl/", "wget/", "crawler", "spider", "bot/"
    };

    private final HeartbeatService service;
    private final KeyGuard keyGuard;
    private final Clock clock;
    private final TrafficRateLimiter trafficLimiter;

    public SentinelController(
            HeartbeatService service,
            KeyGuard keyGuard,
            Clock clock,
            TrafficRateLimiter trafficLimiter) {

        this.service = service;
        this.keyGuard = keyGuard;
        this.clock = clock;
        this.trafficLimiter = trafficLimiter;
    }

    @PostMapping("/vault/ping")
    public SentinelStatus ping(
            @RequestHeader(value = "X-Sentinel-Key",
                    required = false) String key) {

        keyGuard.requireKey(key);
        return service.ping();
    }

    @GetMapping("/vault/status")
    public SentinelStatus status(
            @RequestHeader(value = "X-Sentinel-Key",
                    required = false) String key) {

        keyGuard.requireKey(key);
        return service.getStatus();
    }

    // NEW: Protected traffic statistics endpoint.
    @GetMapping("/vault/traffic")
    public Map<String, Long> traffic(
            @RequestHeader(value = "X-Sentinel-Key",
                    required = false) String key) {

        keyGuard.requireKey(key);
        return trafficLimiter.statistics();
    }

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "sovereign-sentinel",
                "http", "UP"
        );
    }

    @GetMapping("/public/stealth")
    public ResponseEntity<Map<String, Object>> stealth(
            @RequestHeader(value = "User-Agent",
                    required = false) String userAgent,
            @RequestHeader(value = "X-Sentinel-Demo-Bot",
                    required = false) String demoBot,
            HttpServletRequest request) {

        // NEW: Enforce the rate limit before processing.
        // Direct connecting IP is used for this demo.
        TrafficRateLimiter.Decision decision =
                trafficLimiter.check(request.getRemoteAddr());

        if (!decision.allowed()) {
            return ResponseEntity.status(429)
                    .header(
                            "Retry-After",
                            String.valueOf(decision.retryAfterSeconds())
                    )
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                            "mode", "RATE_LIMITED",
                            "message",
                            "Too many requests. Retry after the specified delay.",
                            "retryAfterSeconds",
                            decision.retryAfterSeconds(),
                            "generatedAt",
                            clock.instant().toString()
                    ));
        }

        // Existing User-Agent decoy functionality.
        String ua = userAgent == null
                ? ""
                : userAgent.toLowerCase(Locale.ROOT);

        boolean suspectedAutomation =
                "true".equalsIgnoreCase(demoBot);

        for (String hint : BOT_HINTS) {
            if (ua.contains(hint)) {
                suspectedAutomation = true;
                break;
            }
        }

        Map<String, Object> result;

        if (suspectedAutomation) {
            byte[] bytes = new byte[320];
            RANDOM.nextBytes(bytes);

            result = Map.of(
                    "mode", "DECOY",
                    "classification",
                    "USER_AGENT_HEURISTIC_ONLY",
                    "data",
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(bytes),
                    "generatedAt",
                    clock.instant().toString()
            );
        } else {
            result = Map.of(
                    "mode", "PUBLIC",
                    "message",
                    "Synthetic demonstration endpoint. User-Agent alone cannot prove a request is a bot.",
                    "generatedAt",
                    clock.instant().toString()
            );
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(result);
    }
}
