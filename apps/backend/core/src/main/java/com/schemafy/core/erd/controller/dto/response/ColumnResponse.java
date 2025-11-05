package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.Column;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ColumnResponse {

    private String id;
    private String tableId;
    private String name;
    private String dataType;
    private Integer ordinalPosition;
    private String lengthScale;
    private Boolean isAutoIncrement;
    private String charset;
    private String collation;
    private String comment;

    public static ColumnResponse from(Column column) {
        return ColumnResponse.builder()
                .id(column.getId())
                .tableId(column.getTableId())
                .name(column.getName())
                .dataType(column.getDataType())
                .ordinalPosition(column.getOrdinalPosition())
                .lengthScale(column.getLengthScale())
                .isAutoIncrement(column.isAutoIncrement())
                .charset(column.getCharset())
                .collation(column.getCollation())
                .comment(column.getComment())
                .build();
    }
}
