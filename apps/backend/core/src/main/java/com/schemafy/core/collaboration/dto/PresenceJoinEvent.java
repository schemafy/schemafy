package com.schemafy.core.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceJoinEvent extends PresenceEvent {

    private String userId;
    private String userName;

    public static PresenceJoinEvent of(String sessionId, String userId,
            String userName) {
        return PresenceJoinEvent.builder()
                .type(PresenceEventType.JOIN)
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public PresenceJoinEvent withoutSessionId() {
        return PresenceJoinEvent.builder()
                .type(this.type)
                .sessionId(null)
                .userId(this.userId)
                .userName(this.userName)
                .timestamp(this.timestamp)
                .build();
    }

}
