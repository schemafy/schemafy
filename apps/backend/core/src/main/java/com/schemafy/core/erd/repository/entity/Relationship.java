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
@Table("db_relationships")
public class Relationship extends BaseEntity {
    @Column("src_table_id")
    private String srcTableId;

    @Column("tgt_table_id")
    private String tgtTableId;

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
}
