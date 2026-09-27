package com.uptiq.redis;

import io.lettuce.core.RedisBusyException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Range;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class RedisStreamService {

    private final StringRedisTemplate redisTemplate;

    public RedisStreamService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String xAdd(String stream, Map<String, String> data) {
        StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
        MapRecord<String, String, String> record = MapRecord.create(stream, data);
        RecordId recordId = streamOps.add(record);
        return recordId.getValue();
    }

    public void xAddBulk(String stream, List<Map<String, String>> messages) {
        for (Map<String, String> message : messages) {
            xAdd(stream, message);
        }
    }

    public void createGroup(String stream, String group) {

        try {

            redisTemplate
                    .opsForStream()
                    .createGroup(
                            stream,
                            ReadOffset.from("0-0"),
                            group
                    );

        } catch (RedisSystemException e) {

            if (isBusyGroup(e)) {
                return;
            }

            throw e;
        }
    }

    private boolean isBusyGroup(Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {

            if (current instanceof RedisBusyException) {
                return true;
            }

            String message = current.getMessage();

            if (message != null &&
                    message.contains("BUSYGROUP")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    public List<MapRecord<String, String, String>> xReadGroup(String stream, String group, String consumer, int count, Duration blockTime) {
        StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
        Consumer redisConsumer = Consumer.from(group, consumer);
        StreamReadOptions options = StreamReadOptions.empty()
                        .count(count)
                        .block(blockTime);

        return streamOps.read(
                redisConsumer,
                options,
                StreamOffset.create(
                        stream,
                        ReadOffset.lastConsumed()
                )
        );
    }

    public Long xAck(String stream, String group, String messageId) {
        StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
        return streamOps.acknowledge(
                stream,
                group,
                messageId
        );
    }

    public Long xAckBulk(String stream, String group, List<String> messageIds) {
        StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
        return streamOps.acknowledge(
                stream,
                group,
                messageIds.toArray(new String[0])
        );
    }

    public PendingMessages xPending(String stream, String group, int count, Duration minIdleTime) {
        StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
        return streamOps.pending(
                stream,
                group,
                Range.unbounded(),
                (long) count,
                minIdleTime
        );
    }

    public List<MapRecord<String, String, String>> xClaim(String stream, String group, String newConsumer, Duration minIdleTime, List<String> messageIds) {
        StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
        RecordId[] recordIds = messageIds.stream()
                .map(RecordId::of)
                .toArray(RecordId[]::new);
        RedisStreamCommands.XClaimOptions options =
                RedisStreamCommands.XClaimOptions
                        .minIdle(minIdleTime)
                        .ids(recordIds);
        return streamOps.claim(
                stream,
                group,
                newConsumer,
                options
        );
    }


    public List<MapRecord<String, String, String>> reclaimStuck(String stream, String group, String newConsumer, int count, Duration minIdleTime) {
        PendingMessages pendingMessages = xPending(stream, group, count, minIdleTime);
        List<String> messageIds = pendingMessages.stream()
                .map(PendingMessage::getIdAsString)
                .toList();
        if (messageIds.isEmpty()) {
            return List.of();
        }
        return xClaim(
                stream,
                group,
                newConsumer,
                minIdleTime,
                messageIds
        );
    }
}