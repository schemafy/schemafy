package com.schemafy.core.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PresenceEventType {

    JOIN("JOIN"),
    LEAVE("LEAVE"),
    CURSOR("CURSOR");

    private final String value;

    PresenceEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static PresenceEventType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (PresenceEventType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

}
