package com.schemafy.api.erd.fixture;

import java.util.List;
import java.util.Set;

import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeDefinition;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeParameter;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeParameterName;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeParameterValueType;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeProperties;

public final class DbVendorApiFixture {

  private DbVendorApiFixture() {}

  public static DatatypePolicy mysqlDatatypePolicy() {
    return new DatatypePolicy(
        2,
        "mysql",
        null,
        ">= 8.0 < 9.0",
        List.of(
            new DatatypeDefinition(
                "INT",
                List.of("INTEGER"),
                "INT",
                "numeric_integer",
                List.of(),
                "INT",
                new DatatypeProperties(
                    true,
                    false,
                    Set.of(IndexType.BTREE),
                    "integer")),
            new DatatypeDefinition(
                "VARCHAR",
                List.of(),
                "VARCHAR",
                "string_variable",
                List.of(new DatatypeParameter(
                    DatatypeParameterName.LENGTH,
                    "Length",
                    DatatypeParameterValueType.INTEGER,
                    true,
                    1,
                    0,
                    65_535,
                    null,
                    null,
                    null,
                    null)),
                "VARCHAR({length})",
                new DatatypeProperties(
                    false,
                    true,
                    Set.of(IndexType.BTREE, IndexType.FULLTEXT),
                    "character"))));
  }

}
