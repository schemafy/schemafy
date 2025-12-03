package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

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
    private String dbVendorId;
    private String name;
    private String charset;
    private String collation;
    private String vendorOption;
    private String canvasViewport; // NOTE: 추후 제거 예정
    private Instant createdAt;
    private Instant updatedAt;

    public static SchemaResponse from(Schema schema) {
        return SchemaResponse.builder()
                .id(schema.getId())
                .projectId(schema.getProjectId())
                .dbVendorId(schema.getDbVendorId())
                .name(schema.getName())
                .charset(schema.getCharset())
                .collation(schema.getCollation())
                .vendorOption(schema.getVendorOption())
                .canvasViewport(schema.getCanvasViewport())
                .createdAt(schema.getCreatedAt())
                .updatedAt(schema.getUpdatedAt())
                .build();
    }

}
