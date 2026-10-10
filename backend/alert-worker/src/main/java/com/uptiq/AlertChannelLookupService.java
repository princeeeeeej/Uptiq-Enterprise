package com.uptiq;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlertChannelLookupService {

    private final JdbcTemplate jdbcTemplate;

    public AlertChannelLookupService(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AlertChannelInfo> findActiveChannelsForWebsite(
            UUID websiteId
    ) {

        List<AlertChannelInfo> channels =
                jdbcTemplate.query(
                        """
                        SELECT ac.id, ac.type
                        FROM alert_channels ac
                        JOIN websites w
                          ON w.user_id = ac.user_id
                        WHERE w.id = ?
                          AND ac.is_active = true
                        """,
                        (rs, rowNum) ->
                                new AlertChannelInfo(
                                        rs.getObject(
                                                "id",
                                                UUID.class
                                        ),
                                        rs.getString("type"),
                                        new HashMap<>()
                                ),
                        websiteId
                );

        for (AlertChannelInfo channel : channels) {

            channel.config().putAll(
                    fetchConfig(channel.id())
            );
        }

        return channels;
    }

    private Map<String, String> fetchConfig(
            UUID alertChannelId
    ) {

        Map<String, String> config =
                new HashMap<>();

        jdbcTemplate.query(
                """
                SELECT config_key, config_value
                FROM alert_channel_config
                WHERE alert_channel_id = ?
                """,
                (RowCallbackHandler) rs -> {
                    config.put(
                            rs.getString("config_key"),
                            rs.getString("config_value")
                    );
                },
                alertChannelId
        );

        return config;
    }
}