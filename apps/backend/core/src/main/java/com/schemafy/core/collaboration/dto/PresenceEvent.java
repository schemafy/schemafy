package com.schemafy.core.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PresenceJoinEvent.class, name = "JOIN"),
    @JsonSubTypes.Type(value = PresenceLeaveEvent.class, name = "LEAVE"),
    @JsonSubTypes.Type(value = PresenceCursorEvent.class, name = "CURSOR")
})
public abstract class PresenceEvent {

    protected PresenceEventType type;
    protected String sessionId;
    protected long timestamp;

    public abstract PresenceEvent withoutSessionId();

}
