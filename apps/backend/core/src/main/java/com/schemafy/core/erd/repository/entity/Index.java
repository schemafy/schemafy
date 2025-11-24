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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_indexes")
public class Index extends BaseEntity {

    @Column("table_id")
    private String tableId;

    @Column("name")
    private String name;

    @Column("type")
    private String type;

    @Column("comment")
    private String comment;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static Index newIndex(String tableId, String name, String type,
            String comment) {
        Index index = new Index(tableId, name, type, comment);
        index.setId(UlidGenerator.generate());
        return index;
    }

}
