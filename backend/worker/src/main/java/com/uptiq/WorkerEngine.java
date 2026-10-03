package com.uptiq;

import com.uptiq.redis.RedisMessageFields;
import com.uptiq.redis.RedisStreamConstants;
import com.uptiq.redis.RedisStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WorkerEngine implements SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(WorkerEngine.class);

    private final RedisStreamService redis;
    private final WebsiteCheckService websiteChecker;
    private final WorkerProperties properties;

    private final ExecutorService checkExecutor =
            Executors.newVirtualThreadPerTaskExecutor();

    private final ScheduledExecutorService recoveryExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform()
                            .name("worker-recovery")
                            .factory()
            );

    private final Semaphore concurrency;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread consumerThread;
    private String group;

    public WorkerEngine(
            RedisStreamService redis,
            WebsiteCheckService websiteChecker,
            WorkerProperties properties
    ) {
        this.redis = redis;
        this.websiteChecker = websiteChecker;
        this.properties = properties;
        this.concurrency =
                new Semaphore(properties.getMaxConcurrency());
    }

    @Override
    public void start() {

        if (!running.compareAndSet(false, true)) {
            return;
        }

        group = "uptiq:region:" + properties.getRegionId();

        redis.createGroup(
                RedisStreamConstants.CHECK_STREAM,
                group
        );

        log.info(
                "Worker starting | worker={} | region={} | group={} | concurrency={}",
                properties.getWorkerId(),
                properties.getRegionId(),
                group,
                properties.getMaxConcurrency()
        );

        consumerThread =
                Thread.ofPlatform()
                        .name(
                                "worker-consumer-" +
                                        properties.getWorkerId()
                        )
                        .start(this::consumeLoop);

        recoveryExecutor.scheduleWithFixedDelay(
                this::reclaimStuckJobs,
                properties.getReclaimIntervalMs(),
                properties.getReclaimIntervalMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private void consumeLoop() {

        log.info("Redis consumer loop started");

        while (running.get()) {

            try {

                List<MapRecord<String, String, String>> records =
                        redis.xReadGroup(
                                RedisStreamConstants.CHECK_STREAM,
                                group,
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
                        "Received {} jobs from Redis",
                        records.size()
                );

                for (MapRecord<String, String, String> record : records) {
                    submit(record);
                }

            } catch (Exception e) {

                if (running.get()) {

                    log.error(
                            "Worker consumer error",
                            e
                    );

                    sleepAfterError();
                }
            }
        }
    }

    private void submit(
            MapRecord<String, String, String> record
    ) {

        String messageId =
                record.getId().getValue();

        try {

            concurrency.acquire();

            checkExecutor.submit(() -> {

                try {

                    process(record);

                } finally {

                    concurrency.release();
                }
            });

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.warn(
                    "Worker interrupted while submitting job {}",
                    messageId
            );

        } catch (Exception e) {

            concurrency.release();

            log.error(
                    "Failed to submit job {}",
                    messageId,
                    e
            );
        }
    }

    private void process(
            MapRecord<String, String, String> record
    ) {

        String messageId =
                record.getId().getValue();

        Map<String, String> values =
                record.getValue();

        String websiteId =
                values.get(RedisMessageFields.WEBSITE_ID);

        String url =
                values.get(RedisMessageFields.URL);

        log.info(
                "JOB_RECEIVED | messageId={} | websiteId={} | url={}",
                messageId,
                websiteId,
                url
        );

        if (websiteId == null || url == null || url.isBlank()) {

            log.warn(
                    "Invalid job | messageId={}",
                    messageId
            );

            sendToFailedStream(
                    messageId,
                    "Missing websiteId or url"
            );

            redis.xAck(
                    RedisStreamConstants.CHECK_STREAM,
                    group,
                    messageId
            );

            return;
        }

        try {

            Map<String, String> result =
                    websiteChecker.check(
                            messageId,
                            websiteId,
                            url
                    );

            log.info(
                    "CHECK_COMPLETED | messageId={} | websiteId={} | status={} | statusCode={} | responseTimeMs={}",
                    messageId,
                    websiteId,
                    result.get(RedisMessageFields.STATUS),
                    result.get(RedisMessageFields.STATUS_CODE),
                    result.get(RedisMessageFields.RESPONSE_TIME_MS)
            );

            redis.xAdd(
                    RedisStreamConstants.DB_WRITE_STREAM,
                    result
            );

            log.info(
                    "DB_WRITE_PUBLISHED | messageId={}",
                    messageId
            );

            redis.xAck(
                    RedisStreamConstants.CHECK_STREAM,
                    group,
                    messageId
            );

            log.info(
                    "JOB_ACKED | messageId={}",
                    messageId
            );

        } catch (Exception e) {

            /*
             * DON'T ACK.
             * Redis keeps the message pending.
             */
            log.error(
                    "JOB_FAILED | messageId={}",
                    messageId,
                    e
            );
        }
    }

    private void sendToFailedStream(
            String messageId,
            String reason
    ) {

        redis.xAdd(
                RedisStreamConstants.FAILED_STREAM,
                Map.of(
                        RedisMessageFields.SOURCE_MESSAGE_ID,
                        messageId,
                        RedisMessageFields.ERROR_MESSAGE,
                        reason,
                        RedisMessageFields.WORKER_ID,
                        properties.getWorkerId(),
                        RedisMessageFields.REGION_ID,
                        properties.getRegionId()
                )
        );

        log.warn(
                "JOB_MOVED_TO_FAILED | messageId={} | reason={}",
                messageId,
                reason
        );
    }

    private void reclaimStuckJobs() {

        if (!running.get()) {
            return;
        }

        try {

            List<MapRecord<String, String, String>> records =
                    redis.reclaimStuck(
                            RedisStreamConstants.CHECK_STREAM,
                            group,
                            properties.getWorkerId(),
                            properties.getBatchSize(),
                            Duration.ofMillis(
                                    properties.getClaimMinIdleMs()
                            )
                    );

            if (!records.isEmpty()) {

                log.warn(
                        "RECLAIMED {} stuck jobs",
                        records.size()
                );

                for (MapRecord<String, String, String> record : records) {
                    submit(record);
                }
            }

        } catch (Exception e) {

            log.error(
                    "Recovery error",
                    e
            );
        }
    }

    private void sleepAfterError() {

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

        if (consumerThread != null) {
            consumerThread.interrupt();
        }

        recoveryExecutor.shutdownNow();
        checkExecutor.shutdownNow();

        log.info("Worker stopped");
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