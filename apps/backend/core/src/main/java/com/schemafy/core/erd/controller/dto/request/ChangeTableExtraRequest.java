package com.schemafy.core.erd.controller.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record ChangeTableExtraRequest(
    @JsonDeserialize(using = JsonValueToStringDeserializer.class)
    String extra) {
}
