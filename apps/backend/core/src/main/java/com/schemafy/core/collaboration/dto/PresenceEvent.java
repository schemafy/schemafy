package com.schemafy.core.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PresenceJoinEvent.class, name = "JOIN"),
    @JsonSubTypes.Type(value = PresenceLeaveEvent.class, name = "LEAVE"),
    @JsonSubTypes.Type(value = PresenceCursorEvent.class, name = "CURSOR")
})
public abstract class PresenceEvent {

    protected PresenceEventType type;
    protected String sessionId;
    protected long timestamp;

}
