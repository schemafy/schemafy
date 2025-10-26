package com.schemafy.core.erd.repository.entity;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

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
@Table("db_schemas")
public class Schema extends BaseEntity {

    @Column("project_id")
    private String projectId;

    @Column("db_vendor_id")
    private String dbVendorId;

    @Column("name")
    private String name;

    @Nullable
    @Column("charset")
    private String charset;

    @Nullable
    @Column("collation")
    private String collation;

    @Nullable
    @Column("vendor_option")
    private String vendorOption;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static Schema newSchema(String projectId, String dbVendorId,
            String name,
            String charset, String collation, String vendorOption) {
        Schema schema = new Schema(projectId, dbVendorId, name, charset,
                collation, vendorOption);
        schema.setId(UlidGenerator.generate());
        return schema;
    }

}
