package com.uptiq;

import com.uptiq.redis.RedisMessageFields;
import com.uptiq.redis.RedisStreamConstants;
import com.uptiq.redis.RedisStreamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

@Service
public class AlertStreamPublisher {

    private final RedisStreamService redisStreamService;

    public AlertStreamPublisher(RedisStreamService redisStreamService) {
        this.redisStreamService = redisStreamService;
    }

    public void publish(AlertPayload payload) {
        redisStreamService.xAdd(
                RedisStreamConstants.ALERT_STREAM,
                toMap(payload)
        );
    }

    public void publishAfterCommit(AlertPayload payload) {

        if (TransactionSynchronizationManager.isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            publish(payload);
                        }
                    }
            );

        } else {
            publish(payload);
        }
    }

    private Map<String, String> toMap(AlertPayload payload) {

        Map<String, String> data = new HashMap<>();

        data.put(
                RedisMessageFields.WEBSITE_ID,
                payload.websiteId().toString()
        );

        data.put(
                RedisMessageFields.WEBSITE_NAME,
                payload.websiteName()
        );

        // IMPORTANT:
        // Alerts use websiteUrl, NOT url
        data.put(
                RedisMessageFields.WEBSITE_URL,
                payload.websiteUrl()
        );

        data.put(
                RedisMessageFields.EVENT_TYPE,
                payload.eventType().name()
        );

        if (payload.reason() != null) {
            data.put(
                    RedisMessageFields.REASON,
                    payload.reason()
            );
        }

        if (payload.daysRemaining() != null) {
            data.put(
                    RedisMessageFields.DAYS_REMAINING,
                    payload.daysRemaining().toString()
            );
        }

        return data;
    }
}