package com.sovereign.sentinel.scheduler;

import com.sovereign.sentinel.service.HeartbeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatScheduler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatScheduler.class);
    private final HeartbeatService heartbeatService;

    public HeartbeatScheduler(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @Scheduled(
            initialDelayString = "${sentinel.scheduler.interval-ms:60000}",
            fixedDelayString = "${sentinel.scheduler.interval-ms:60000}"
    )
    public void checkHeartbeat() {
        if (heartbeatService.checkAndLockIfExpired()) {
            log.warn("Heartbeat expired: state transitioned to LOCKDOWN");
        }
    }
}
