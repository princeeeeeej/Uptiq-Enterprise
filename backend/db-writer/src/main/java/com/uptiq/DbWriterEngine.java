package com.uptiq;

import com.uptiq.redis.RedisStreamConstants;
import com.uptiq.redis.RedisStreamService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DbWriterEngine implements SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(DbWriterEngine.class);

    private final RedisStreamService redis;
    private final DbWriterProperties properties;
    private final DbWriterService dbWriterService;

    private final AtomicBoolean running =
            new AtomicBoolean(false);

    private Thread consumerThread;

    private ScheduledExecutorService recoveryExecutor;

    public DbWriterEngine(
            RedisStreamService redis,
            DbWriterProperties properties,
            DbWriterService dbWriterService
    ) {
        this.redis = redis;
        this.properties = properties;
        this.dbWriterService = dbWriterService;
    }

    @Override
    public void start() {

        if (!running.compareAndSet(false, true)) {
            return;
        }

        redis.createGroup(
                RedisStreamConstants.DB_WRITE_STREAM,
                properties.getGroup()
        );

        log.info(
                "DB Writer started | worker={} | group={}",
                properties.getWorkerId(),
                properties.getGroup()
        );

        // Main consumer
        consumerThread =
                Thread.ofPlatform()
                        .name("db-writer-" + properties.getWorkerId())
                        .start(this::consumeLoop);

        // Recovery worker
        recoveryExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        Thread.ofPlatform()
                                .name("db-writer-recovery-" + properties.getWorkerId())
                                .factory()
                );

        recoveryExecutor.scheduleWithFixedDelay(
                this::recoverStuckMessages,
                properties.getReclaimIntervalMs(),
                properties.getReclaimIntervalMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private void consumeLoop() {

        while (running.get()) {

            try {

                List<MapRecord<String, String, String>> records =
                        redis.xReadGroup(
                                RedisStreamConstants.DB_WRITE_STREAM,
                                properties.getGroup(),
                                properties.getWorkerId(),
                                properties.getBatchSize(),
                                Duration.ofMillis(
                                        properties.getBlockMs()
                                )
                        );

                if (records.isEmpty()) {
                    continue;
                }

                log.info(
                        "Received {} DB write jobs",
                        records.size()
                );

                processBatch(records);

            } catch (Exception e) {

                if (running.get()) {

                    log.error(
                            "DB Writer consumer error",
                            e
                    );

                    sleep();
                }
            }
        }
    }

    private void recoverStuckMessages() {

        if (!running.get()) {
            return;
        }

        try {

            List<MapRecord<String, String, String>> records =
                    redis.reclaimStuck(
                            RedisStreamConstants.DB_WRITE_STREAM,
                            properties.getGroup(),
                            properties.getWorkerId(),
                            properties.getReclaimBatchSize(),
                            Duration.ofMillis(
                                    properties.getClaimMinIdleMs()
                            )
                    );

            if (records.isEmpty()) {
                return;
            }

            log.warn(
                    "Recovered {} stuck DB write jobs",
                    records.size()
            );

            processBatch(records);

        } catch (Exception e) {

            if (running.get()) {

                log.error(
                        "DB Writer recovery failed",
                        e
                );
            }
        }
    }

    private void processBatch(
            List<MapRecord<String, String, String>> records
    ) {

        try {

            dbWriterService.persistBatch(records);

            List<String> messageIds =
                    records.stream()
                            .map(record ->
                                    record.getId().getValue()
                            )
                            .toList();

            redis.xAckBulk(
                    RedisStreamConstants.DB_WRITE_STREAM,
                    properties.getGroup(),
                    messageIds
            );

            log.info(
                    "DB batch committed + ACKed | count={}",
                    records.size()
            );

            /*
             * SSL check runs after DB commit + ACK.
             * A slow TLS check cannot break the tick pipeline.
             */
            dbWriterService.checkAndUpdateSSL(records);

        } catch (Exception e) {

            /*
             * DO NOT ACK on failure.
             *
             * Redis keeps the message pending.
             * Recovery worker can claim it later.
             */
            log.error(
                    "Failed to process DB write batch",
                    e
            );
        }
    }

    private void sleep() {

        try {

            Thread.sleep(1000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {

        if (!running.compareAndSet(true, false)) {
            return;
        }

        // Stop main consumer
        if (consumerThread != null) {
            consumerThread.interrupt();
        }

        // Stop recovery scheduler
        if (recoveryExecutor != null) {
            recoveryExecutor.shutdownNow();
        }

        log.info("DB Writer stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}