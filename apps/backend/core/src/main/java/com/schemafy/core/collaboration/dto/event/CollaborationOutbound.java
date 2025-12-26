package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JoinEvent.Outbound.class, name = "JOIN"),
    @JsonSubTypes.Type(value = LeaveEvent.Outbound.class, name = "LEAVE"),
    @JsonSubTypes.Type(value = CursorEvent.Outbound.class, name = "CURSOR"),
    @JsonSubTypes.Type(value = SchemaFocusEvent.Outbound.class, name = "SCHEMA_FOCUS"),
    @JsonSubTypes.Type(value = ChatEvent.Outbound.class, name = "CHAT")
})
public sealed interface CollaborationOutbound
        permits JoinEvent.Outbound, LeaveEvent.Outbound, CursorEvent.Outbound,
                SchemaFocusEvent.Outbound, ChatEvent.Outbound {

    CollaborationEventType type();

    String sessionId();

    long timestamp();

    CollaborationOutbound withoutSessionId();

}
