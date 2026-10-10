package com.uptiq;

import com.uptiq.entity.AlertEventType;
import com.uptiq.entity.AlertType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertDispatchService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AlertDispatchService.class
            );

    private final AlertChannelLookupService channelLookupService;
    private final AlertConditionService conditionService;
    private final List<AlertSender> senders;

    private Map<AlertType, AlertSender> senderMap;

    public AlertDispatchService(
            AlertChannelLookupService channelLookupService,
            AlertConditionService conditionService,
            List<AlertSender> senders
    ) {
        this.channelLookupService =
                channelLookupService;

        this.conditionService =
                conditionService;

        this.senders = senders;
    }

    @PostConstruct
    void buildSenderMap() {

        senderMap =
                new EnumMap<>(AlertType.class);

        for (AlertSender sender : senders) {

            senderMap.put(
                    sender.supports(),
                    sender
            );
        }
    }

    public void dispatch(
            AlertPayload payload
    ) {

        List<AlertChannelInfo> channels =
                channelLookupService
                        .findActiveChannelsForWebsite(
                                payload.websiteId()
                        );

        if (channels.isEmpty()) {
            return;
        }

        for (AlertChannelInfo channel : channels) {

            boolean shouldFire =
                    conditionService.shouldFire(
                            payload.websiteId(),
                            channel.id(),
                            payload.eventType()
                    );

            if (!shouldFire) {
                continue;
            }

            AlertType type =
                    AlertType.valueOf(
                            channel.type()
                    );

            AlertSender sender =
                    senderMap.get(type);

            if (sender == null) {

                log.warn(
                        "No sender registered for {}",
                        type
                );

                continue;
            }

            /*
             * Important:
             * let exceptions escape.
             *
             * AlertWorkerEngine will NOT ACK the
             * Redis message if delivery fails.
             */
            sender.send(
                    channel,
                    payload
            );

            conditionService.logEvent(
                    payload.websiteId(),
                    channel.id(),
                    payload.eventType()
            );

            log.info(
                    "Alert sent | website={} channel={} type={}",
                    payload.websiteId(),
                    channel.id(),
                    payload.eventType()
            );
        }
    }
}