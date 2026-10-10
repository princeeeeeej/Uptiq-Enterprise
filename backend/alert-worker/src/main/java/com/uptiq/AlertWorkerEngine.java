package com.uptiq;

import com.uptiq.entity.AlertEventType;
import com.uptiq.redis.RedisMessageFields;
import com.uptiq.redis.RedisStreamConstants;
import com.uptiq.redis.RedisStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AlertWorkerEngine implements SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(AlertWorkerEngine.class);

    private static final String STREAM =
            RedisStreamConstants.ALERT_STREAM;

    private final RedisStreamService redisStreamService;
    private final AlertWorkerProperties properties;
    private final AlertDispatchService alertDispatchService;
    private final AlertRetryService retryService;
    private final AlertDeadLetterService deadLetterService;

    private final ExecutorService consumerExecutor =
            Executors.newSingleThreadExecutor(
                    Thread.ofPlatform()
                            .name("alert-worker-consumer")
                            .factory()
            );

    private final ScheduledExecutorService recoveryExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform()
                            .name("alert-worker-recovery")
                            .factory()
            );

    private volatile boolean running = false;

    private ScheduledFuture<?> recoveryTask;

    public AlertWorkerEngine(
            RedisStreamService redisStreamService,
            AlertWorkerProperties properties,
            AlertDispatchService alertDispatchService,
            AlertRetryService retryService,
            AlertDeadLetterService deadLetterService
    ) {
        this.redisStreamService = redisStreamService;
        this.properties = properties;
        this.alertDispatchService = alertDispatchService;
        this.retryService = retryService;
        this.deadLetterService = deadLetterService;
    }

    @Override
    public synchronized void start() {

        if (running) {
            return;
        }

        try {
            redisStreamService.createGroup(
                    STREAM,
                    properties.getGroup()
            );

            running = true;

            consumerExecutor.submit(this::consumeLoop);

            recoveryTask = recoveryExecutor.scheduleWithFixedDelay(
                    this::recoverStuckJobs,
                    properties.getReclaimIntervalMs(),
                    properties.getReclaimIntervalMs(),
                    TimeUnit.MILLISECONDS
            );

            log.info(
                    "Alert Worker started | worker={} | group={}",
                    properties.getWorkerId(),
                    properties.getGroup()
            );

        } catch (Exception e) {

            running = false;

            log.error(
                    "Failed to start Alert Worker",
                    e
            );

            throw e;
        }
    }

    private void consumeLoop() {

        log.info(
                "Alert consumer loop started | worker={}",
                properties.getWorkerId()
        );

        while (running) {

            try {

                List<MapRecord<String, String, String>> records =
                        redisStreamService.xReadGroup(
                                STREAM,
                                properties.getGroup(),
                                properties.getWorkerId(),
                                properties.getBatchSize(),
                                Duration.ofMillis(
                                        properties.getBlockMs()
                                )
                        );

                if (records == null || records.isEmpty()) {
                    continue;
                }

                log.info(
                        "Received {} alert jobs",
                        records.size()
                );

                for (MapRecord<String, String, String> record : records) {

                    if (!running) {
                        break;
                    }

                    processRecord(record);
                }

            } catch (Exception e) {

                if (!running) {
                    break;
                }

                log.error(
                        "Alert Worker consumer error",
                        e
                );

                /*
                 * Prevent a tight loop if Redis temporarily fails.
                 */
                sleepQuietly(1000);
            }
        }

        log.info("Alert consumer loop stopped");
    }

    private void processRecord(
            MapRecord<String, String, String> record
    ) {

        String messageId =
                record.getId().getValue();

        try {

            AlertPayload payload =
                    parsePayload(record.getValue());

            /*
             * Main business logic.
             *
             * If sending fails, AlertDispatchService throws.
             */
            alertDispatchService.dispatch(payload);

            /*
             * SUCCESS
             *
             * ACK only after alert delivery completed successfully.
             */
            redisStreamService.xAck(
                    STREAM,
                    properties.getGroup(),
                    messageId
            );

            /*
             * Successful message no longer needs
             * a retry counter.
             */
            retryService.clear(
                    STREAM,
                    messageId
            );

            log.info(
                    "Alert job completed | messageId={} | event={}",
                    messageId,
                    payload.eventType()
            );

        } catch (Exception e) {

            handleFailure(
                    record,
                    e
            );
        }
    }

    private void handleFailure(
            MapRecord<String, String, String> record,
            Exception exception
    ) {

        String messageId =
                record.getId().getValue();

        int attempt =
                retryService.incrementAttempt(
                        STREAM,
                        messageId
                );

        log.error(
                "Alert delivery failed | messageId={} | attempt={} | error={}",
                messageId,
                attempt,
                exception.getMessage(),
                exception
        );

        /*
         * Retry until max attempts.
         */
        if (!retryService.shouldMoveToDlq(attempt)) {

            /*
             * IMPORTANT:
             *
             * Do NOT ACK here.
             *
             * The message remains PENDING in Redis.
             * The recovery loop will later XCLAIM it.
             */
            log.warn(
                    "Alert job will be retried | messageId={} | attempt={}",
                    messageId,
                    attempt
            );

            return;
        }

        /*
         * Maximum attempts reached.
         *
         * Move message to DLQ and ACK the original.
         */
        try {

            deadLetterService.moveToDlq(
                    STREAM,
                    properties.getGroup(),
                    record,
                    attempt,
                    exception
            );

            retryService.clear(
                    STREAM,
                    messageId
            );

            log.error(
                    "Alert moved to DLQ | messageId={} | attempts={}",
                    messageId,
                    attempt
            );

        } catch (Exception dlqException) {

            /*
             * Very important:
             *
             * If DLQ publishing fails, do NOT ACK
             * the original message.
             *
             * It remains pending and can be recovered.
             */
            log.error(
                    "Failed to move alert to DLQ | messageId={} | attempt={}",
                    messageId,
                    attempt,
                    dlqException
            );
        }
    }

    private void recoverStuckJobs() {

        if (!running) {
            return;
        }

        try {

            List<MapRecord<String, String, String>> recovered =
                    redisStreamService.reclaimStuck(
                            STREAM,
                            properties.getGroup(),
                            properties.getWorkerId(),
                            properties.getReclaimBatchSize(),
                            Duration.ofMillis(
                                    properties.getClaimMinIdleMs()
                            )
                    );

            if (recovered == null || recovered.isEmpty()) {
                return;
            }

            log.warn(
                    "Recovered {} stuck alert jobs",
                    recovered.size()
            );

            for (MapRecord<String, String, String> record : recovered) {

                if (!running) {
                    break;
                }

                processRecord(record);
            }

        } catch (Exception e) {

            if (!running) {
                return;
            }

            log.error(
                    "Alert recovery failed",
                    e
            );
        }
    }

    private AlertPayload parsePayload(
            Map<String, String> data
    ) {

        String websiteId =
                required(
                        data,
                        RedisMessageFields.WEBSITE_ID
                );

        String websiteName =
                required(
                        data,
                        RedisMessageFields.WEBSITE_NAME
                );

        /*
         * New alert messages use:
         *
         * websiteUrl
         *
         * Older messages may still contain:
         *
         * url
         *
         * Keep fallback during migration.
         */
        String websiteUrl =
                data.get(
                        RedisMessageFields.WEBSITE_URL
                );

        if (websiteUrl == null ||
                websiteUrl.isBlank()) {

            websiteUrl =
                    data.get(
                            RedisMessageFields.URL
                    );
        }

        if (websiteUrl == null ||
                websiteUrl.isBlank()) {

            throw new IllegalArgumentException(
                    "Missing alert field: websiteUrl"
            );
        }

        String eventType =
                required(
                        data,
                        RedisMessageFields.EVENT_TYPE
                );

        String reason =
                data.get(
                        RedisMessageFields.REASON
                );

        String daysRemainingRaw =
                data.get(
                        RedisMessageFields.DAYS_REMAINING
                );

        Integer daysRemaining = null;

        if (daysRemainingRaw != null &&
                !daysRemainingRaw.isBlank()) {

            try {

                daysRemaining =
                        Integer.valueOf(
                                daysRemainingRaw
                        );

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "Invalid daysRemaining: "
                                + daysRemainingRaw,
                        e
                );
            }
        }

        UUID websiteUuid;

        try {

            websiteUuid =
                    UUID.fromString(websiteId);

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid websiteId: "
                            + websiteId,
                    e
            );
        }

        AlertEventType alertEventType;

        try {

            alertEventType =
                    AlertEventType.valueOf(
                            eventType
                    );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid eventType: "
                            + eventType,
                    e
            );
        }

        return new AlertPayload(
                websiteUuid,
                websiteName,
                websiteUrl,
                alertEventType,
                reason,
                daysRemaining
        );
    }

    private String required(
            Map<String, String> data,
            String field
    ) {

        String value =
                data.get(field);

        if (value == null ||
                value.isBlank()) {

            throw new IllegalArgumentException(
                    "Missing alert field: "
                            + field
            );
        }

        return value;
    }

    private void sleepQuietly(long millis) {

        try {

            Thread.sleep(millis);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {

        if (!running) {
            return;
        }

        running = false;

        if (recoveryTask != null) {
            recoveryTask.cancel(false);
        }

        consumerExecutor.shutdownNow();
        recoveryExecutor.shutdownNow();

        log.info("Alert Worker stopped");
    }

    @Override
    public void stop(Runnable callback) {

        stop();

        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}