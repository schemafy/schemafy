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
@Table("db_relationships")
public class Relationship extends BaseEntity {

    @Column("fk_table_id")
    private String fkTableId;

    @Column("pk_table_id")
    private String pkTableId;

    @Column("name")
    private String name;

    @Column("kind")
    private String kind;

    @Column("cardinality")
    private String cardinality;

    @Column("on_delete")
    private String onDelete;

    @Column("on_update")
    private String onUpdate;

    @Column("extra")
    private String extra;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static Relationship newRelationship(String fkTableId,
            String pkTableId,
            String name, String kind, String cardinality, String onDelete,
            String onUpdate, String extra) {
        Relationship relationship = new Relationship(fkTableId, pkTableId,
                name,
                kind, cardinality, onDelete, onUpdate, extra);
        relationship.setId(UlidGenerator.generate());
        return relationship;
    }

}
