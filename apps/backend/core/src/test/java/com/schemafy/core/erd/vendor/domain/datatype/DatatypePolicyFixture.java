package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.List;
import java.util.Set;

import com.schemafy.core.erd.index.domain.type.IndexType;

public final class DatatypePolicyFixture {

  private DatatypePolicyFixture() {}

  public static DatatypePolicy mysqlPolicy() {
    return new DatatypePolicy(
        2,
        "mysql",
        null,
        ">= 8.0 < 9.0",
        List.of(
            definition(
                "INT",
                List.of("INTEGER"),
                List.of(),
                "INT",
                new DatatypeProperties(true, false, Set.of(IndexType.BTREE), "integer")),
            definition(
                "BIGINT",
                List.of(),
                List.of(),
                "BIGINT",
                new DatatypeProperties(true, false, Set.of(IndexType.BTREE), "bigint")),
            definition(
                "DECIMAL",
                List.of("NUMERIC", "DEC", "FIXED"),
                List.of(
                    integerParameter(DatatypeParameterName.PRECISION, false, 1, 1, 65),
                    integerParameter(DatatypeParameterName.SCALE, false, 2, 0, 30)),
                "DECIMAL[({precision}[, {scale}])]",
                new DatatypeProperties(false, false, Set.of(IndexType.BTREE), "decimal")),
            definition(
                "CHAR",
                List.of(),
                List.of(integerParameter(DatatypeParameterName.LENGTH, false, 1, 0, 255)),
                "CHAR[({length})]",
                new DatatypeProperties(false, true, Set.of(IndexType.BTREE, IndexType.FULLTEXT), "character")),
            definition(
                "TEXT",
                List.of(),
                List.of(),
                "TEXT",
                new DatatypeProperties(false, true, Set.of(IndexType.FULLTEXT), null)),
            definition(
                "VARCHAR",
                List.of(),
                List.of(integerParameter(DatatypeParameterName.LENGTH, true, 1, 0, 65_535)),
                "VARCHAR({length})",
                new DatatypeProperties(false, true, Set.of(IndexType.BTREE, IndexType.FULLTEXT), "character")),
            definition(
                "ENUM",
                List.of(),
                List.of(arrayParameter(DatatypeParameterName.VALUES, true, 1, 1, 65_535, 1, 255)),
                "ENUM({values})",
                new DatatypeProperties(false, true, Set.of(IndexType.BTREE), "character")),
            definition(
                "BIT",
                List.of(),
                List.of(integerParameter(DatatypeParameterName.LENGTH, false, 1, 1, 64)),
                "BIT[({length})]",
                new DatatypeProperties(false, false, Set.of(IndexType.BTREE), "bit")),
            definition(
                "DATETIME",
                List.of(),
                List.of(integerParameter(DatatypeParameterName.LENGTH, false, 1, 0, 6)),
                "DATETIME[({length})]",
                new DatatypeProperties(false, false, Set.of(IndexType.BTREE), "datetime")),
            definition(
                "SET",
                List.of(),
                List.of(arrayParameter(DatatypeParameterName.VALUES, true, 1, 1, 64, 1, 255)),
                "SET({values})",
                new DatatypeProperties(false, true, Set.of(IndexType.BTREE), "character")),
            definition(
                "POINT",
                List.of(),
                List.of(),
                "POINT",
                new DatatypeProperties(false, false, Set.of(IndexType.SPATIAL), null))));
  }

  public static DatatypeDefinition definition(
      String sqlType,
      List<String> aliases,
      List<DatatypeParameter> parameters,
      String template,
      DatatypeProperties properties) {
    return new DatatypeDefinition(
        sqlType,
        aliases,
        sqlType,
        "test",
        parameters,
        template,
        properties);
  }

  public static DatatypeParameter integerParameter(
      DatatypeParameterName name,
      boolean required,
      int order,
      int min,
      int max) {
    return new DatatypeParameter(
        name,
        name.jsonName(),
        DatatypeParameterValueType.INTEGER,
        required,
        order,
        min,
        max,
        null,
        null,
        null,
        null);
  }

  public static DatatypeParameter arrayParameter(
      DatatypeParameterName name,
      boolean required,
      int order,
      int minItems,
      int maxItems,
      int minItemLength,
      int maxItemLength) {
    return new DatatypeParameter(
        name,
        name.jsonName(),
        DatatypeParameterValueType.STRING_ARRAY,
        required,
        order,
        null,
        null,
        minItems,
        maxItems,
        minItemLength,
        maxItemLength);
  }

}
