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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_vendors")
public class DbVendor extends BaseEntity {

    @Column("display_name")
    protected String id;

    @Column("name")
    private String name;

    @Column("version")
    private String version;

    @Column("datatype_mappings")
    private String datatypeMappings;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static DbVendor newDbVendor(String displayName, String name,
            String version, String datatypeMappings) {
        DbVendor vendor = new DbVendor();
        vendor.setId(displayName);
        vendor.setName(name);
        vendor.setVersion(version);
        vendor.setDatatypeMappings(datatypeMappings);
        return vendor;
    }

}
