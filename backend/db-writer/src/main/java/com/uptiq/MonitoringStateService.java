package com.uptiq;

import com.uptiq.entity.AlertEventType;
import com.uptiq.entity.WebsiteStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class MonitoringStateService {

    private final JdbcTemplate jdbcTemplate;
    private final AlertStreamPublisher alertStreamPublisher;
    private final DbWriterProperties properties;

    public MonitoringStateService(
            JdbcTemplate jdbcTemplate,
            AlertStreamPublisher alertStreamPublisher,
            DbWriterProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.alertStreamPublisher = alertStreamPublisher;
        this.properties = properties;
    }

    public void updateState(
            UUID websiteId,
            Instant checkedAt,
            Integer responseTimeMs
    ) {

        LocalDateTime checkedAtUtc =
                LocalDateTime.ofInstant(
                        checkedAt,
                        ZoneOffset.UTC
                );

        /*
         * Lock website row.
         *
         * This serializes state + incident updates
         * for the same website.
         */
        jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM websites
                WHERE id = ?
                FOR UPDATE
                """,
                UUID.class,
                websiteId
        );

        ConsensusResult consensus =
                calculateConsensus(websiteId);

        CurrentState current =
                getCurrentState(websiteId);

        WebsiteStatus nextStatus =
                consensus.status();

        /*
         * We are intentionally NOT changing
         * consecutiveFails here.
         *
         * You asked to skip that part for now.
         */
        int nextConsecutiveFails =
                current.consecutiveFails();

        if (nextStatus == WebsiteStatus.UP) {
            nextConsecutiveFails = 0;
        }

        upsertCurrentStatus(
                websiteId,
                nextStatus,
                nextConsecutiveFails,
                checkedAtUtc,
                responseTimeMs,
                consensus.consensusRegions()
        );

        updateIncident(
                websiteId,
                nextStatus,
                checkedAtUtc,
                consensus
        );
    }

    private ConsensusResult calculateConsensus(
            UUID websiteId
    ) {

        List<RegionStatus> latestStatuses =
                jdbcTemplate.query(
                        """
                        SELECT DISTINCT ON (region_id)
                               region_id,
                               status
                        FROM website_ticks
                        WHERE website_id = ?
                        ORDER BY region_id, checked_at DESC, id DESC
                        """,
                        (rs, rowNum) ->
                                new RegionStatus(
                                        rs.getObject(
                                                "region_id",
                                                UUID.class
                                        ),
                                        WebsiteStatus.valueOf(
                                                rs.getString("status")
                                        )
                                ),
                        websiteId
                );

        int upCount =
                (int) latestStatuses.stream()
                        .filter(region ->
                                region.status()
                                        == WebsiteStatus.UP
                        )
                        .count();

        int downCount =
                (int) latestStatuses.stream()
                        .filter(region ->
                                region.status()
                                        == WebsiteStatus.DOWN
                        )
                        .count();

        int observedRegions =
                latestStatuses.size();

        int threshold =
                properties.getConsensusThreshold();

        int totalRegions =
                properties.getTotalRegions();

        validateConsensusConfig(
                threshold,
                totalRegions
        );

        /*
         * Example:
         *
         * threshold = 2
         *
         * UP
         * UP
         * DOWN
         *
         * => UP
         */
        if (upCount >= threshold) {

            return new ConsensusResult(
                    WebsiteStatus.UP,
                    upCount,
                    observedRegions
            );
        }

        /*
         * Example:
         *
         * DOWN
         * DOWN
         * UP
         *
         * => DOWN
         */
        if (downCount >= threshold) {

            return new ConsensusResult(
                    WebsiteStatus.DOWN,
                    downCount,
                    observedRegions
            );
        }

        /*
         * Conflicting observations.
         *
         * Example:
         *
         * UP
         * DOWN
         * UNKNOWN
         *
         * No consensus.
         */
        if (upCount > 0 && downCount > 0) {

            return new ConsensusResult(
                    WebsiteStatus.REGIONAL_ANOMALY,
                    0,
                    observedRegions
            );
        }

        /*
         * We have observations but nobody reached
         * the configured threshold.
         */
        if (observedRegions > 0) {

            return new ConsensusResult(
                    WebsiteStatus.UNKNOWN,
                    0,
                    observedRegions
            );
        }

        return new ConsensusResult(
                WebsiteStatus.UNKNOWN,
                0,
                0
        );
    }

    private void validateConsensusConfig(
            int threshold,
            int totalRegions
    ) {

        if (threshold < 1) {

            throw new IllegalStateException(
                    "dbwriter.consensus-threshold must be >= 1"
            );
        }

        if (totalRegions < 1) {

            throw new IllegalStateException(
                    "dbwriter.total-regions must be >= 1"
            );
        }

        if (threshold > totalRegions) {

            throw new IllegalStateException(
                    "dbwriter.consensus-threshold cannot be greater " +
                            "than dbwriter.total-regions"
            );
        }
    }

    private CurrentState getCurrentState(
            UUID websiteId
    ) {

        List<CurrentState> states =
                jdbcTemplate.query(
                        """
                        SELECT current_status,
                               consecutive_fails
                        FROM website_status_current
                        WHERE website_id = ?
                        """,
                        (rs, rowNum) ->
                                new CurrentState(
                                        WebsiteStatus.valueOf(
                                                rs.getString(
                                                        "current_status"
                                                )
                                        ),
                                        rs.getInt(
                                                "consecutive_fails"
                                        )
                                ),
                        websiteId
                );

        if (states.isEmpty()) {

            return new CurrentState(
                    WebsiteStatus.UNKNOWN,
                    0
            );
        }

        return states.getFirst();
    }

    private void upsertCurrentStatus(
            UUID websiteId,
            WebsiteStatus status,
            int consecutiveFails,
            LocalDateTime checkedAt,
            Integer responseTimeMs,
            int consensusRegions
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO website_status_current
                (
                    website_id,
                    current_status,
                    consecutive_fails,
                    last_checked_at,
                    last_response_time_ms,
                    consensus_regions
                )
                VALUES (?, ?, ?, ?, ?, ?)

                ON CONFLICT (website_id)
                DO UPDATE SET
                    current_status =
                        EXCLUDED.current_status,

                    consecutive_fails =
                        EXCLUDED.consecutive_fails,

                    last_checked_at =
                        EXCLUDED.last_checked_at,

                    last_response_time_ms =
                        EXCLUDED.last_response_time_ms,

                    consensus_regions =
                        EXCLUDED.consensus_regions
                """,
                websiteId,
                status.name(),
                consecutiveFails,
                Timestamp.valueOf(checkedAt),
                responseTimeMs,
                consensusRegions
        );
    }

    private void updateIncident(
            UUID websiteId,
            WebsiteStatus nextStatus,
            LocalDateTime checkedAt,
            ConsensusResult consensus
    ) {

        /*
         * Confirmed DOWN.
         */
        if (nextStatus == WebsiteStatus.DOWN) {

            Integer openIncidents =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM incidents
                            WHERE website_id = ?
                              AND resolved_at IS NULL
                            """,
                            Integer.class,
                            websiteId
                    );

            /*
             * Do not create duplicate open incidents.
             */
            if (openIncidents != null &&
                    openIncidents > 0) {

                return;
            }

            String reason =
                    "DOWN consensus: " +
                            consensus.consensusRegions() +
                            " region(s)";

            jdbcTemplate.update(
                    """
                    INSERT INTO incidents
                    (
                        id,
                        website_id,
                        started_at,
                        reason
                    )
                    VALUES
                    (
                        gen_random_uuid(),
                        ?,
                        ?,
                        ?
                    )
                    """,
                    websiteId,
                    Timestamp.valueOf(checkedAt),
                    reason
            );

            WebsiteBasicInfo website =
                    getWebsiteInfo(websiteId);

            /*
             * IMPORTANT:
             *
             * Do not send email here.
             *
             * Publish an alert job to Redis AFTER
             * the PostgreSQL transaction commits.
             */
            alertStreamPublisher.publishAfterCommit(
                    new AlertPayload(
                            websiteId,
                            website.name(),
                            website.url(),
                            AlertEventType.DOWN,
                            reason,
                            null
                    )
            );

            return;
        }

        /*
         * Only confirmed UP resolves
         * an existing incident.
         *
         * REGIONAL_ANOMALY does NOT resolve it.
         */
        if (nextStatus == WebsiteStatus.UP) {

            int resolvedRows =
                    jdbcTemplate.update(
                            """
                            UPDATE incidents
                            SET resolved_at = ?,
                                duration_seconds =
                                    EXTRACT(
                                        EPOCH FROM (
                                            ? - started_at
                                        )
                                    )::INTEGER
                            WHERE website_id = ?
                              AND resolved_at IS NULL
                            """,
                            Timestamp.valueOf(checkedAt),
                            Timestamp.valueOf(checkedAt),
                            websiteId
                    );

            /*
             * Send RECOVERED only when an actual
             * open incident was resolved.
             */
            if (resolvedRows > 0) {

                WebsiteBasicInfo website =
                        getWebsiteInfo(websiteId);

                alertStreamPublisher.publishAfterCommit(
                        new AlertPayload(
                                websiteId,
                                website.name(),
                                website.url(),
                                AlertEventType.RECOVERED,
                                null,
                                null
                        )
                );
            }
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

    private record RegionStatus(
            UUID regionId,
            WebsiteStatus status
    ) {
    }

    private record CurrentState(
            WebsiteStatus status,
            int consecutiveFails
    ) {
    }

    private record ConsensusResult(
            WebsiteStatus status,
            int consensusRegions,
            int observedRegions
    ) {
    }

    private record WebsiteBasicInfo(
            String name,
            String url
    ) {
    }
}