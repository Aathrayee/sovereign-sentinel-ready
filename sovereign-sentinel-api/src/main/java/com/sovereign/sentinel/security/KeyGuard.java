package com.sovereign.sentinel.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KeyGuard {
    private final byte[] expected;

    public KeyGuard(@Value("${sentinel.api-key}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SENTINEL_API_KEY must not be empty");
        }
        this.expected = secret.getBytes(StandardCharsets.UTF_8);
    }

    public void requireKey(String suppliedKey) {
        if (suppliedKey == null ||
                !MessageDigest.isEqual(expected, suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing X-Sentinel-Key");
        }
    }
}
