package com.schemafy.core.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceLeaveEvent extends PresenceEvent {

    private String userId;
    private String userName;

    public static PresenceLeaveEvent of(String sessionId, String userId,
            String userName) {
        return PresenceLeaveEvent.builder()
                .type(PresenceEventType.LEAVE)
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public PresenceLeaveEvent withoutSessionId() {
        return PresenceLeaveEvent.builder()
                .type(this.type)
                .sessionId(null)
                .userId(this.userId)
                .userName(this.userName)
                .timestamp(this.timestamp)
                .build();
    }

}
