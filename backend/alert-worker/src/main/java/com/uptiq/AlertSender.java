package com.uptiq;

import com.uptiq.entity.AlertType;

public interface AlertSender {

    AlertType supports();

    void send(
            AlertChannelInfo channel,
            AlertPayload payload
    );
}