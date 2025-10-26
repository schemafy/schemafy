package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

import com.schemafy.core.erd.repository.entity.Schema;

public record GetSchemasResponse(
    List<Schema> schemas
) {

    public static GetSchemasResponse of(List<Schema> schemas) {
        return new GetSchemasResponse(schemas);
    }
}
