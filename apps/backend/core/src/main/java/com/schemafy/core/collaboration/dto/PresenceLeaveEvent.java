package com.schemafy.core.collaboration.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class PresenceLeaveEvent extends PresenceEvent {

    public static PresenceLeaveEvent of(String sessionId) {
        return PresenceLeaveEvent.builder()
                .type(PresenceEventType.LEAVE)
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
