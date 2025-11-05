package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.Schema;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SchemaResponse {

    private String id;
    private String projectId;
    private String name;

    public static SchemaResponse from(Schema schema) {
        return SchemaResponse.builder()
                .id(schema.getId())
                .projectId(schema.getProjectId())
                .name(schema.getName())
                .build();
    }
}
