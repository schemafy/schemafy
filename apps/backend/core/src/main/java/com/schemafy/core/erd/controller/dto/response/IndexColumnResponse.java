package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.core.erd.repository.entity.IndexColumn;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexColumnResponse {

    private String id;
    private String indexId;
    private String columnId;
    private Integer seqNo;
    private String sortDir;

    public static IndexColumnResponse from(IndexColumn indexColumn) {
        return IndexColumnResponse.builder()
                .id(indexColumn.getId())
                .indexId(indexColumn.getIndexId())
                .columnId(indexColumn.getColumnId())
                .seqNo(indexColumn.getSeqNo())
                .sortDir(indexColumn.getSortDir())
                .build();
    }

}
