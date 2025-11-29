package com.schemafy.core.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceCursorEvent extends PresenceEvent {

    private CursorPosition cursor;

    public static PresenceCursorEvent of(String sessionId,
            CursorPosition cursor) {
        return PresenceCursorEvent.builder()
                .type(PresenceEventType.CURSOR)
                .sessionId(sessionId)
                .cursor(cursor)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
