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
@Table("db_relationship_columns")
public class RelationshipColumn extends BaseEntity {
    @Column("relationship_id")
    private String relationshipId;

    @Column("src_column_id")
    private String srcColumnId;

    @Column("tgt_column_id")
    private String tgtColumnId;

    @Column("seq_no")
    private int seqNo;
}
