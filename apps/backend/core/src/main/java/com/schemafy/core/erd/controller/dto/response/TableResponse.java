package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableResponse {

    private String id;
    private String schemaId;
    private String name;
    private String extra;

    public static TableResponse from(Table table) {
        return TableResponse.builder()
                .id(table.getId())
                .schemaId(table.getSchemaId())
                .name(table.getName())
                .extra(table.getExtra())
                .build();
    }
}
