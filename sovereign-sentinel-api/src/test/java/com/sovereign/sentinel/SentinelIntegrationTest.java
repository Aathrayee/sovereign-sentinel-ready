package com.sovereign.sentinel;

import com.sovereign.sentinel.model.HeartbeatRecord;
import com.sovereign.sentinel.model.SystemState;
import com.sovereign.sentinel.repository.HeartbeatRepository;
import com.sovereign.sentinel.scheduler.HeartbeatScheduler;
import com.sovereign.sentinel.service.HeartbeatService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sentineltest;DB_CLOSE_DELAY=-1",
        "sentinel.api-key=test-secret"
})
@AutoConfigureMockMvc
class SentinelIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HeartbeatService service;
    @Autowired HeartbeatRepository repository;
    @Autowired HeartbeatScheduler scheduler;

    @BeforeEach
    void reset() {
        service.ping();
    }

    @Test
    void keyIsRequired() throws Exception {
        mvc.perform(get("/api/vault/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pingIsActive() throws Exception {
        mvc.perform(post("/api/vault/ping")
                        .header("X-Sentinel-Key", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void schedulerTransitionsToLockdownWhenExpired() throws Exception {
        HeartbeatRecord record = repository.findById(1L).orElseThrow();
        record.setLastPingAt(Instant.now().minus(Duration.ofHours(25)));
        record.setStatus(SystemState.ACTIVE);
        repository.saveAndFlush(record);

        scheduler.checkHeartbeat();

        mvc.perform(get("/api/vault/status")
                        .header("X-Sentinel-Key", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKDOWN"))
                .andExpect(jsonPath("$.secondsRemaining").value(0));
    }

    @Test
    void suspectedBotGetsSyntheticNoise() throws Exception {
        mvc.perform(get("/api/public/stealth")
                        .header("User-Agent", "GPTBot/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("DECOY"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }
}
