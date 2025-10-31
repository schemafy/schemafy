package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import com.schemafy.core.common.type.BaseEntity;

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
@Builder
@Table("db_columns")
public class Column extends BaseEntity {

    @org.springframework.data.relational.core.mapping.Column("table_id")
    private String tableId;

    @org.springframework.data.relational.core.mapping.Column("name")
    private String name;

    @org.springframework.data.relational.core.mapping.Column("ordinal_position")
    private int ordinalPosition;

    @org.springframework.data.relational.core.mapping.Column("data_type")
    private String dataType;

    @Nullable
    @org.springframework.data.relational.core.mapping.Column("length_scale")
    private String lengthScale;

    @Nullable
    @org.springframework.data.relational.core.mapping.Column("is_auto_increment")
    private boolean isAutoIncrement;

    @Nullable
    @org.springframework.data.relational.core.mapping.Column("charset")
    private String charset;

    @Nullable
    @org.springframework.data.relational.core.mapping.Column("collation")
    private String collation;

    @Nullable
    @Builder.Default
    @org.springframework.data.relational.core.mapping.Column("nullable")
    private boolean nullable = true;

    @Nullable
    @org.springframework.data.relational.core.mapping.Column("comment")
    private String comment;

}
