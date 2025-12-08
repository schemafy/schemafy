package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;
import java.util.List;

import com.schemafy.core.erd.repository.entity.Schema;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SchemaDetailResponse {

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
    private List<TableResponse> tables;

    public static SchemaDetailResponse from(Schema schema,
            List<TableResponse> tables) {
        return SchemaDetailResponse.builder()
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
                .tables(tables)
                .build();
    }

}
