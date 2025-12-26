package com.schemafy.core.collaboration.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.schemafy.core.collaboration.dto.CollaborationEventType;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CursorEvent.Inbound.class, name = "CURSOR"),
    @JsonSubTypes.Type(value = SchemaFocusEvent.Inbound.class, name = "SCHEMA_FOCUS"),
    @JsonSubTypes.Type(value = ChatEvent.Inbound.class, name = "CHAT")
})
public sealed interface CollaborationInbound
        permits CursorEvent.Inbound, SchemaFocusEvent.Inbound, ChatEvent.Inbound {

    CollaborationEventType type();

}
