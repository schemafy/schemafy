package com.schemafy.core.erd.controller.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record ChangeRelationshipExtraRequest(
    @JsonDeserialize(using = JsonValueToStringDeserializer.class) String extra) {
}
