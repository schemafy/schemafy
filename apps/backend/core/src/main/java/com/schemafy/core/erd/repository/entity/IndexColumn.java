package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_index_columns")
public class IndexColumn extends BaseEntity {
    @Column("index_id")
    private String indexId;

    @Column("column_id")
    private String columnId;

    @Column("seq_no")
    private int seqNo;

    @Column("sort_dir")
    private String sortDir;
}
