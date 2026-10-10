package com.uptiq;

import com.uptiq.entity.AlertEventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AlertConditionService {

    private static final long SSL_COOLDOWN_HOURS = 24;

    private final JdbcTemplate jdbcTemplate;

    public AlertConditionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean shouldFire(
            UUID websiteId,
            UUID alertChannelId,
            AlertEventType eventType
    ) {

        LastAlertEvent last =
                getLastEvent(
                        websiteId,
                        alertChannelId
                );

        if (last == null) {
            return true;
        }

        if (eventType == AlertEventType.DOWN &&
                last.eventType() == AlertEventType.DOWN) {

            return false;
        }

        if (eventType == AlertEventType.RECOVERED &&
                last.eventType() == AlertEventType.RECOVERED) {

            return false;
        }

        if (eventType == AlertEventType.SSL_EXPIRING) {

            long hours =
                    Duration.between(
                            last.createdAt(),
                            LocalDateTime.now()
                    ).toHours();

            return hours >= SSL_COOLDOWN_HOURS;
        }

        return true;
    }

    public void logEvent(
            UUID websiteId,
            UUID alertChannelId,
            AlertEventType eventType
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO alert_events
                (
                    id,
                    website_id,
                    alert_channel_id,
                    event_type,
                    created_at
                )
                VALUES
                (
                    gen_random_uuid(),
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                websiteId,
                alertChannelId,
                eventType.name(),
                Timestamp.valueOf(
                        LocalDateTime.now()
                )
        );
    }

    private LastAlertEvent getLastEvent(
            UUID websiteId,
            UUID alertChannelId
    ) {

        List<LastAlertEvent> results =
                jdbcTemplate.query(
                        """
                        SELECT event_type, created_at
                        FROM alert_events
                        WHERE website_id = ?
                          AND alert_channel_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                        (rs, rowNum) ->
                                new LastAlertEvent(
                                        AlertEventType.valueOf(
                                                rs.getString(
                                                        "event_type"
                                                )
                                        ),
                                        rs.getTimestamp(
                                                "created_at"
                                        ).toLocalDateTime()
                                ),
                        websiteId,
                        alertChannelId
                );

        return results.isEmpty()
                ? null
                : results.getFirst();
    }

    private record LastAlertEvent(
            AlertEventType eventType,
            LocalDateTime createdAt
    ) {
    }
}