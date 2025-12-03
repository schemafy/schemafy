package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static RelationshipColumn newRelationshipColumn(
            String relationshipId,
            String srcColumnId, String tgtColumnId, int seqNo) {
        RelationshipColumn relationshipColumn = new RelationshipColumn(
                relationshipId,
                srcColumnId, tgtColumnId, seqNo);
        relationshipColumn.setId(UlidGenerator.generate());
        return relationshipColumn;
    }

}
