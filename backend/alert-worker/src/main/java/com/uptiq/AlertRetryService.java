package com.uptiq;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlertRetryService {

    private static final String KEY_PREFIX = "uptiq:retry:";

    private final StringRedisTemplate redisTemplate;
    private final AlertRetryProperties properties;

    public AlertRetryService(
            StringRedisTemplate redisTemplate,
            AlertRetryProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public int incrementAttempt(
            String stream,
            String messageId
    ) {

        String key = retryKey(stream, messageId);

        Long count = redisTemplate.opsForValue().increment(key);

        return count == null ? 1 : count.intValue();
    }

    public void clear(
            String stream,
            String messageId
    ) {

        redisTemplate.delete(
                retryKey(stream, messageId)
        );
    }

    public boolean shouldMoveToDlq(int attempt) {
        return attempt >= properties.getMaxAttempts();
    }

    private String retryKey(
            String stream,
            String messageId
    ) {

        return KEY_PREFIX
                + stream
                + ":"
                + messageId;
    }
}