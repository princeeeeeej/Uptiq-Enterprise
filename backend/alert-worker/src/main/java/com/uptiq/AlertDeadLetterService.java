package com.uptiq;

import com.uptiq.redis.RedisMessageFields;
import com.uptiq.redis.RedisStreamConstants;
import com.uptiq.redis.RedisStreamService;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AlertDeadLetterService {

    private final RedisStreamService redisStreamService;

    public AlertDeadLetterService(
            RedisStreamService redisStreamService
    ) {
        this.redisStreamService = redisStreamService;
    }

    public void moveToDlq(
            String originalStream,
            String originalGroup,
            MapRecord<String, String, String> record,
            int attempt,
            Exception exception
    ) {

        Map<String, String> data =
                new HashMap<>(record.getValue());

        String originalMessageId =
                record.getId().getValue();

        data.put(
                RedisMessageFields.ORIGINAL_STREAM,
                originalStream
        );

        data.put(
                RedisMessageFields.ORIGINAL_MESSAGE_ID,
                originalMessageId
        );

        data.put(
                RedisMessageFields.RETRY_COUNT,
                String.valueOf(attempt)
        );

        data.put(
                RedisMessageFields.LAST_ERROR,
                safeErrorMessage(exception)
        );

        redisStreamService.xAdd(
                RedisStreamConstants.FAILED_STREAM,
                data
        );

        redisStreamService.xAck(
                originalStream,
                originalGroup,
                originalMessageId
        );
    }

    private String safeErrorMessage(Exception exception) {

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message.length() > 500
                ? message.substring(0, 500)
                : message;
    }
}