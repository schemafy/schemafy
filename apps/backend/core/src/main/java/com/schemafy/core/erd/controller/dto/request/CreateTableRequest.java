package com.schemafy.core.erd.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record CreateTableRequest(
    @NotBlank(message = "schemaId는 필수입니다.") String schemaId,
    @NotBlank(message = "name은 필수입니다.") String name,
    String charset,
    String collation,
    @JsonDeserialize(using = JsonValueToStringDeserializer.class) String extra) {
}
