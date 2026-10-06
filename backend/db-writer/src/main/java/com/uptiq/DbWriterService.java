package com.uptiq;

import com.uptiq.entity.AlertEventType;
import com.uptiq.entity.WebsiteStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.connection.stream.MapRecord;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DbWriterService {

    private static final String INSERT_TICK_SQL =
            """
            INSERT INTO website_ticks
            (
                id,
                website_id,
                region_id,
                source_message_id,
                status,
                status_code,
                response_time_ms,
                error_message,
                checked_at
            )
            VALUES
            (
                gen_random_uuid(),
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            )
            ON CONFLICT (source_message_id)
            DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MonitoringStateService monitoringStateService;
    private final SSLCertChecker sslCertChecker;
    private final AlertStreamPublisher alertStreamPublisher;
    private final DbWriterProperties properties;

    public DbWriterService(
            JdbcTemplate jdbcTemplate,
            MonitoringStateService monitoringStateService,
            SSLCertChecker sslCertChecker,
            AlertStreamPublisher alertStreamPublisher,
            DbWriterProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.monitoringStateService =
                monitoringStateService;
        this.sslCertChecker =
                sslCertChecker;
        this.alertStreamPublisher =
                alertStreamPublisher;
        this.properties =
                properties;
    }

    @Transactional
    public void persistBatch(
            List<MapRecord<String, String, String>> records
    ) {

        for (MapRecord<String, String, String> record :
                records) {

            persistRecord(record);
        }
    }

    private void persistRecord(
            MapRecord<String, String, String> record
    ) {

        Map<String, String> data =
                record.getValue();

        /*
         * Parse website ID.
         */
        UUID websiteId =
                UUID.fromString(
                        required(
                                data,
                                "websiteId"
                        )
                );

        /*
         * Parse region ID.
         */
        UUID regionId =
                UUID.fromString(
                        required(
                                data,
                                "regionId"
                        )
                );

        /*
         * Worker status.
         */
        WebsiteStatus status =
                WebsiteStatus.valueOf(
                        required(
                                data,
                                "status"
                        )
                );

        String sourceMessageId =
                required(
                        data,
                        "sourceMessageId"
                );

        Integer statusCode =
                parseInteger(
                        data.get("statusCode")
                );

        Integer responseTimeMs =
                parseInteger(
                        data.get("responseTimeMs")
                );

        String errorMessage =
                data.get("errorMessage");

        Instant checkedAt =
                Instant.parse(
                        required(
                                data,
                                "checkedAt"
                        )
                );

        /*
         * Insert tick.
         *
         * source_message_id is UNIQUE.
         *
         * Therefore retries are idempotent.
         */
        int insertedRows =
                jdbcTemplate.update(
                        INSERT_TICK_SQL,
                        websiteId,
                        regionId,
                        sourceMessageId,
                        status.name(),
                        statusCode,
                        responseTimeMs,
                        errorMessage,
                        Timestamp.from(checkedAt)
                );

        /*
         * If this message was already written,
         * do not update monitoring state again.
         *
         * This prevents duplicate Redis deliveries
         * from changing current state twice.
         */
        if (insertedRows == 0) {
            return;
        }

        /*
         * Recalculate website state after
         * storing the new regional tick.
         */
        monitoringStateService.updateState(
                websiteId,
                checkedAt,
                responseTimeMs
        );
    }

    /*
     * SSL checking happens AFTER the normal DB
     * pipeline has finished.
     *
     * The DbWriterEngine calls this after ACK.
     */
    public void checkAndUpdateSSL(
            List<MapRecord<String, String, String>> records
    ) {

        /*
         * One website can appear multiple times
         * in the same batch.
         *
         * Do SSL check only once per website.
         */
        Set<UUID> websiteIds =
                new LinkedHashSet<>();

        for (MapRecord<String, String, String> record :
                records) {

            String websiteId =
                    record.getValue()
                            .get("websiteId");

            if (websiteId == null ||
                    websiteId.isBlank()) {

                continue;
            }

            try {

                websiteIds.add(
                        UUID.fromString(websiteId)
                );

            } catch (IllegalArgumentException ignored) {
                // malformed message was already handled
                // by the DB pipeline
            }
        }

        for (UUID websiteId : websiteIds) {

            try {

                checkWebsiteSSL(websiteId);

            } catch (Exception e) {

                /*
                 * SSL monitoring should never crash
                 * the DB writer.
                 */
                System.err.println(
                        "SSL check failed | website=" +
                                websiteId +
                                " | error=" +
                                e.getMessage()
                );
            }
        }
    }

    private void checkWebsiteSSL(
            UUID websiteId
    ) {

        WebsiteBasicInfo website =
                getWebsiteInfo(websiteId);

        /*
         * Only HTTPS needs certificate checking.
         */
        if (website.url() == null ||
                !website.url()
                        .toLowerCase()
                        .startsWith("https://")) {

            return;
        }

        /*
         * SSLCertChecker performs:
         *
         * HTTPS connection
         * certificate handshake
         * issuer extraction
         * validity extraction
         * days remaining calculation
         */
        var certificate =
                sslCertChecker.check(
                        website.url()
                );

        /*
         * null means certificate check failed.
         */
        if (certificate == null) {
            return;
        }

        Timestamp now =
                Timestamp.from(
                        Instant.now()
                );

        /*
         * Save / update SSL status.
         */
        jdbcTemplate.update(
                """
                INSERT INTO website_ssl_status
                (
                    website_id,
                    issuer,
                    valid_from,
                    valid_until,
                    days_remaining,
                    last_checked_at
                )
                VALUES (?, ?, ?, ?, ?, ?)

                ON CONFLICT (website_id)
                DO UPDATE SET
                    issuer =
                        EXCLUDED.issuer,

                    valid_from =
                        EXCLUDED.valid_from,

                    valid_until =
                        EXCLUDED.valid_until,

                    days_remaining =
                        EXCLUDED.days_remaining,

                    last_checked_at =
                        EXCLUDED.last_checked_at
                """,
                websiteId,
                certificate.issuer(),
                Timestamp.from(
                        certificate.validFrom()
                ),
                Timestamp.from(
                        certificate.validUntil()
                ),
                certificate.daysRemaining(),
                now
        );

        int threshold =
                properties
                        .getSslExpiringThresholdDays();

        /*
         * Publish SSL alert job.
         *
         * Alert Worker will decide:
         *
         * 1. Which channels are active
         * 2. Whether cooldown allows the alert
         * 3. How to send it
         */
        if (certificate.daysRemaining()
                <= threshold) {

            alertStreamPublisher.publish(
                    new AlertPayload(
                            websiteId,
                            website.name(),
                            website.url(),
                            AlertEventType.SSL_EXPIRING,
                            null,
                            certificate.daysRemaining()
                    )
            );
        }
    }

    private WebsiteBasicInfo getWebsiteInfo(
            UUID websiteId
    ) {

        return jdbcTemplate.queryForObject(
                """
                SELECT name, url
                FROM websites
                WHERE id = ?
                """,
                (rs, rowNum) ->
                        new WebsiteBasicInfo(
                                rs.getString("name"),
                                rs.getString("url")
                        ),
                websiteId
        );
    }

    private String required(
            Map<String, String> data,
            String key
    ) {

        String value =
                data.get(key);

        if (value == null ||
                value.isBlank()) {

            throw new IllegalArgumentException(
                    "Missing required field: " + key
            );
        }

        return value;
    }

    private Integer parseInteger(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        return Integer.valueOf(value);
    }

    private record WebsiteBasicInfo(
            String name,
            String url
    ) {
    }
}