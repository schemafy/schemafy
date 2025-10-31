package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Column;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@org.springframework.data.relational.core.mapping.Table("db_tables")
public class Table extends BaseEntity {

    @Column("schema_id")
    private String schemaId;

    @Column("name")
    private String name;

    @Column("comment")
    private String comment;

    @Column("table_options")
    private String tableOptions;

    @Column("extra")
    private String extra;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static Table newTable(String schemaId, String name, String comment,
            String tableOptions, String extra) {
        Table table = new Table(schemaId, name, comment, tableOptions, extra);
        table.setId(UlidGenerator.generate());
        return table;
    }

}
