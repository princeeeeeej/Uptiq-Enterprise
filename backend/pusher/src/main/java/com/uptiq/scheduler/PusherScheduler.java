package com.uptiq.scheduler;

import com.uptiq.service.PusherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PusherScheduler {

    private static final Logger log = LoggerFactory.getLogger(PusherScheduler.class);
    private final PusherService pusherService;

    public PusherScheduler(PusherService pusherService) {
        this.pusherService = pusherService;
    }

    @Scheduled(
            fixedDelayString = "${pusher.interval-ms:180000}",
            initialDelayString = "${pusher.initial-delay-ms:10000}"
    )
    public void pushChecks() {
        long start = System.currentTimeMillis();
        try {
            int count = pusherService.pushWebsiteChecks();
            long duration = System.currentTimeMillis() - start;
            log.info(
                    "Scheduled {} websites in {} ms",
                    count,
                    duration
            );
        } catch (Exception e) {
            log.error("Failed to schedule website checks", e);
        }
    }
}