package com.schemafy.core.erd.ddl.domain.mysql;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlGenerator;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.Column;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.Constraint;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.ConstraintColumn;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.ConstraintSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.IndexColumn;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.IndexSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.Relationship;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.RelationshipColumn;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.RelationshipSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.Table;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.TableSnapshot;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;
import com.schemafy.core.erd.index.domain.type.IndexType;

@Component
public class MySqlDdlGenerator implements DdlGenerator {

  private static final int MAX_IDENTIFIER_LENGTH = 64;
  private static final int MAX_COLUMN_COMMENT_LENGTH = 1024;
  private static final int MAX_BIT_LENGTH = 64;
  private static final int MAX_CHAR_LENGTH = 255;
  private static final int MAX_VARCHAR_LENGTH = 65_535;
  private static final int MAX_TEMPORAL_FSP = 6;
  private static final int MAX_INTEGER_DISPLAY_WIDTH = 255;
  private static final int MAX_FLOAT_PRECISION_BITS = 53;
  private static final int MAX_ENUM_VALUES = 65_535;
  private static final int MAX_SET_VALUES = 64;
  private static final int MAX_ENUM_SET_VALUE_LENGTH = 255;

  // MySQL parses SET DEFAULT, but InnoDB rejects it for foreign keys.
  private static final Set<String> VALID_REFERENTIAL_ACTIONS = Set.of(
      "CASCADE", "SET NULL", "RESTRICT", "NO ACTION");

  // This generator always emits ENGINE=InnoDB. HASH is not a valid InnoDB
  // index algorithm.
  private static final Set<IndexType> VALID_INDEX_TYPES = Set.of(
      IndexType.BTREE, IndexType.FULLTEXT, IndexType.SPATIAL);

  private static final Set<String> SUPPORTED_DATA_TYPES = Set.of(
      "TINYINT", "SMALLINT", "MEDIUMINT", "INT", "INTEGER", "BIGINT",
      "FLOAT", "DOUBLE", "REAL", "DECIMAL", "NUMERIC", "BIT", "BOOL",
      "BOOLEAN", "CHAR", "VARCHAR", "TINYTEXT", "TEXT", "MEDIUMTEXT",
      "LONGTEXT", "BINARY", "VARBINARY", "BLOB", "TINYBLOB",
      "MEDIUMBLOB", "LONGBLOB", "ENUM", "SET", "DATE", "TIME",
      "DATETIME", "TIMESTAMP", "YEAR", "GEOMETRY", "POINT",
      "LINESTRING", "POLYGON", "MULTIPOINT", "MULTILINESTRING",
      "MULTIPOLYGON", "GEOMETRYCOLLECTION", "JSON");

  private static final Set<String> INTEGER_TYPES = Set.of(
      "TINYINT", "SMALLINT", "MEDIUMINT", "INT", "INTEGER", "BIGINT");

  private static final Set<String> REQUIRED_LENGTH_TYPES = Set.of(
      "VARCHAR", "VARBINARY");

  private static final Set<String> OPTIONAL_LENGTH_TYPES = Set.of(
      "TINYINT", "SMALLINT", "MEDIUMINT", "INT", "INTEGER", "BIGINT",
      "BIT", "CHAR", "BINARY", "FLOAT", "TIME", "DATETIME", "TIMESTAMP",
      "YEAR");

  private static final Set<String> TEMPORAL_FSP_TYPES = Set.of(
      "TIME", "DATETIME", "TIMESTAMP");

  private static final Set<String> PRECISION_SCALE_TYPES = Set.of(
      "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE", "REAL");

  private static final Set<String> CHARACTER_STRING_TYPES = Set.of(
      "CHAR", "VARCHAR", "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT",
      "ENUM", "SET");

  private static final Set<String> BINARY_STRING_TYPES = Set.of(
      "BINARY", "VARBINARY");

  private static final Set<String> TEXT_OR_BLOB_TYPES = Set.of(
      "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT", "TINYBLOB", "BLOB",
      "MEDIUMBLOB", "LONGBLOB");

  private static final Set<String> FULLTEXT_TYPES = Set.of(
      "CHAR", "VARCHAR", "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT");

  private static final Set<String> SPATIAL_TYPES = Set.of(
      "GEOMETRY", "POINT", "LINESTRING", "POLYGON", "MULTIPOINT",
      "MULTILINESTRING", "MULTIPOLYGON", "GEOMETRYCOLLECTION");

  private static final Set<String> VALUE_LIST_TYPES = Set.of("ENUM", "SET");

  @Override
  public DdlExportVendor exportVendor() {
    return DdlExportVendor.MYSQL;
  }

  @Override
  public String generate(DdlSchemaSnapshot snapshot) {
    requireSnapshot(snapshot);
    requireMysqlCompatible(snapshot.schema().dbVendorName());

    List<TableSnapshot> tables = normalizeTables(snapshot.tables());
    DdlContext context = DdlContext.from(tables);

    StringBuilder ddl = new StringBuilder();
    ddl.append(generateHeader(snapshot.schema()));
    ddl.append(generateSchemaStatement(snapshot.schema()));

    if (!tables.isEmpty()) {
      ddl.append("\n\n");
    }

    List<String> createStatements = tables.stream()
        .map(table -> generateCreateTable(table, context))
        .toList();
    ddl.append(String.join("\n\n", createStatements));

    List<String> alterStatements = generateAlterStatements(tables, context);
    if (!alterStatements.isEmpty()) {
      ddl.append("\n\n");
      ddl.append(String.join("\n", alterStatements));
    }

    return ddl.toString();
  }

  private String generateHeader(SchemaSnapshot schema) {
    return "-- Schemafy MySQL DDL Export\n"
        + "-- Schema: " + escapeComment(schema.name()) + "\n"
        + "-- Vendor: " + escapeComment(schema.dbVendorName()) + "\n\n";
  }

  private String generateSchemaStatement(SchemaSnapshot schema) {
    StringBuilder ddl = new StringBuilder();
    ddl.append("CREATE SCHEMA IF NOT EXISTS ")
        .append(quoteIdentifier(schema.name()));
    sanitizeOptionalIdentifier(schema.charset(), "Schema charset")
        .ifPresent(charset -> ddl.append(" DEFAULT CHARACTER SET ")
            .append(charset));
    sanitizeOptionalIdentifier(schema.collation(), "Schema collation")
        .ifPresent(collation -> ddl.append(" COLLATE ")
            .append(collation));
    ddl.append(";\n");
    ddl.append("USE ").append(quoteIdentifier(schema.name())).append(";");
    return ddl.toString();
  }

  private String generateCreateTable(TableSnapshot snapshot,
      DdlContext context) {
    Table table = requireTable(snapshot);
    requireIdentifier(table.name(), "Table name");

    if (snapshot.columns().isEmpty()) {
      throw invalid("Table '%s' must contain at least one column"
          .formatted(table.name()));
    }

    StringBuilder ddl = new StringBuilder();
    ddl.append("CREATE TABLE ")
        .append(quoteIdentifier(table.name()))
        .append(" (\n");

    List<String> clauses = new ArrayList<>();
    ColumnRules columnRules = ColumnRules.from(snapshot, context);
    for (Column column : sortColumns(snapshot.columns())) {
      clauses.add(generateColumnDefinition(column, columnRules));
    }

    primaryKeyClause(snapshot, context)
        .ifPresent(clauses::add);

    clauses.addAll(checkClauses(snapshot));

    ddl.append(String.join(",\n", clauses));
    ddl.append("\n)");
    ddl.append(tableOptions(table));
    ddl.append(";");
    return ddl.toString();
  }

  private String generateColumnDefinition(Column column,
      ColumnRules columnRules) {
    requireColumn(column);
    String dataType = sanitizeDataType(column.dataType());
    validateColumnDefinition(column, dataType, columnRules);

    StringBuilder ddl = new StringBuilder("  ");
    ddl.append(quoteIdentifier(column.name()))
        .append(" ")
        .append(formatDataType(dataType, column.typeArguments()));

    sanitizeOptionalIdentifier(column.charset(), "Column charset")
        .ifPresent(charset -> ddl.append(" CHARACTER SET ").append(charset));
    sanitizeOptionalIdentifier(column.collation(), "Column collation")
        .ifPresent(collation -> ddl.append(" COLLATE ").append(collation));

    if (columnRules.notNullColumnIds().contains(column.id())) {
      ddl.append(" NOT NULL");
    }

    String defaultExpr = columnRules.defaultExpressionsByColumnId()
        .get(column.id());
    if (defaultExpr != null) {
      ddl.append(" DEFAULT ").append(sanitizeExpression(defaultExpr,
          "Default expression"));
    }

    if (column.autoIncrement()) {
      if (!columnRules.leftmostKeyColumnIds().contains(column.id())) {
        throw invalid("AUTO_INCREMENT column '%s' must be the first column of a key"
            .formatted(column.name()));
      }
      ddl.append(" AUTO_INCREMENT");
    }

    if (column.comment() != null && !column.comment().isBlank()) {
      if (column.comment().length() > MAX_COLUMN_COMMENT_LENGTH) {
        throw invalid("Column comment must be at most "
            + MAX_COLUMN_COMMENT_LENGTH + " characters");
      }
      ddl.append(" COMMENT '")
          .append(escapeString(column.comment()))
          .append("'");
    }

    return ddl.toString();
  }

  private String formatDataType(String dataType,
      ColumnTypeArguments arguments) {
    if (arguments == null || arguments.isEmpty()) {
      return dataType;
    }
    if (arguments.hasLength()) {
      return dataType + "(" + arguments.length() + ")";
    }
    if (arguments.hasPrecisionScale()) {
      return dataType + "(" + arguments.precision() + ","
          + arguments.scale() + ")";
    }
    if (arguments.hasValues()) {
      String values = arguments.values().stream()
          .map(value -> "'" + escapeString(value) + "'")
          .collect(Collectors.joining(", "));
      return dataType + "(" + values + ")";
    }
    return dataType;
  }

  private Optional<String> primaryKeyClause(TableSnapshot snapshot,
      DdlContext context) {
    List<ConstraintSnapshot> primaryKeys = constraintsOf(
        snapshot, ConstraintKind.PRIMARY_KEY);
    if (primaryKeys.isEmpty()) {
      return Optional.empty();
    }
    if (primaryKeys.size() > 1) {
      throw invalid("Table '%s' has multiple primary key constraints"
          .formatted(snapshot.table().name()));
    }

    List<ConstraintColumn> columns = requireConstraintColumns(
        primaryKeys.getFirst(), "Primary key");
    validateKeyColumns(
        requireTable(snapshot), columns.stream()
            .sorted(comparingSeqNo(ConstraintColumn::seqNo,
                ConstraintColumn::id))
            .map(ConstraintColumn::columnId)
            .toList(),
        context, "Primary key");
    String columnList = columns.stream()
        .sorted(comparingSeqNo(ConstraintColumn::seqNo,
            ConstraintColumn::id))
        .map(column -> quoteColumn(context, snapshot.table().id(),
            column.columnId()))
        .collect(Collectors.joining(", "));

    return Optional.of("  PRIMARY KEY (" + columnList + ")");
  }

  private List<String> checkClauses(TableSnapshot snapshot) {
    return constraintsOf(snapshot, ConstraintKind.CHECK).stream()
        .sorted(comparingNullableStrings(
            constraint -> constraint.constraint().name(),
            constraint -> constraint.constraint().id()))
        .map(this::checkClause)
        .toList();
  }

  private String checkClause(ConstraintSnapshot snapshot) {
    Constraint constraint = requireConstraint(snapshot);
    String expression = sanitizeExpression(
        requireNonBlank(constraint.checkExpr(), "Check expression"),
        "Check expression");

    if (constraint.name() == null || constraint.name().isBlank()) {
      return "  CHECK (" + expression + ")";
    }
    return "  CONSTRAINT " + quoteIdentifier(constraint.name())
        + " CHECK (" + expression + ")";
  }

  private String tableOptions(Table table) {
    StringBuilder options = new StringBuilder(" ENGINE=InnoDB");
    sanitizeOptionalIdentifier(table.charset(), "Table charset")
        .ifPresent(charset -> options.append(" DEFAULT CHARSET=")
            .append(charset));
    sanitizeOptionalIdentifier(table.collation(), "Table collation")
        .ifPresent(collation -> options.append(" COLLATE=")
            .append(collation));
    return options.toString();
  }

  private List<String> generateAlterStatements(List<TableSnapshot> tables,
      DdlContext context) {
    List<String> statements = new ArrayList<>();
    for (TableSnapshot table : tables) {
      statements.addAll(uniqueKeyStatements(table, context));
      statements.addAll(indexStatements(table, context));
    }
    for (TableSnapshot table : tables) {
      statements.addAll(foreignKeyStatements(table, context));
    }
    return statements;
  }

  private List<String> uniqueKeyStatements(TableSnapshot snapshot,
      DdlContext context) {
    Table table = requireTable(snapshot);
    return constraintsOf(snapshot, ConstraintKind.UNIQUE).stream()
        .sorted(comparingNullableStrings(
            constraint -> constraint.constraint().name(),
            constraint -> constraint.constraint().id()))
        .map(constraint -> uniqueKeyStatement(snapshot, constraint, context))
        .toList();
  }

  private String uniqueKeyStatement(TableSnapshot tableSnapshot,
      ConstraintSnapshot snapshot,
      DdlContext context) {
    Table table = requireTable(tableSnapshot);
    Constraint constraint = requireConstraint(snapshot);
    requireIdentifier(constraint.name(), "Unique constraint name");

    List<ConstraintColumn> constraintColumns = requireConstraintColumns(
        snapshot, constraint.name()).stream()
        .sorted(comparingSeqNo(ConstraintColumn::seqNo,
            ConstraintColumn::id))
        .toList();
    validateKeyColumns(
        table,
        constraintColumns.stream()
            .map(ConstraintColumn::columnId)
            .toList(),
        context,
        "Unique constraint '%s'".formatted(constraint.name()));

    String columns = constraintColumns.stream()
        .map(column -> quoteColumn(context, table.id(), column.columnId()))
        .collect(Collectors.joining(", "));

    return "ALTER TABLE " + quoteIdentifier(table.name())
        + " ADD UNIQUE KEY " + quoteIdentifier(constraint.name())
        + " (" + columns + ");";
  }

  private List<String> indexStatements(TableSnapshot snapshot,
      DdlContext context) {
    Table table = requireTable(snapshot);
    return snapshot.indexes().stream()
        .map(MySqlDdlGenerator::requireIndex)
        .sorted(comparingNullableStrings(
            index -> index.index().name(),
            index -> index.index().id()))
        .map(index -> indexStatement(snapshot, index, context))
        .toList();
  }

  private String indexStatement(TableSnapshot tableSnapshot,
      IndexSnapshot snapshot,
      DdlContext context) {
    Table table = requireTable(tableSnapshot);
    var index = snapshot.index();
    requireIdentifier(index.name(), "Index name");
    if (index.type() == null || !VALID_INDEX_TYPES.contains(index.type())) {
      throw invalid("Unsupported MySQL index type: " + index.type());
    }
    if (snapshot.columns().isEmpty()) {
      throw invalid("Index '%s' must contain at least one column"
          .formatted(index.name()));
    }
    validateIndexColumns(tableSnapshot, snapshot, context);

    StringBuilder ddl = new StringBuilder();
    ddl.append("ALTER TABLE ")
        .append(quoteIdentifier(table.name()))
        .append(" ADD ");

    if (index.type() == IndexType.FULLTEXT) {
      ddl.append("FULLTEXT ");
    } else if (index.type() == IndexType.SPATIAL) {
      ddl.append("SPATIAL ");
    }

    ddl.append("INDEX ")
        .append(quoteIdentifier(index.name()))
        .append(" (")
        .append(snapshot.columns().stream()
            .sorted(comparingSeqNo(IndexColumn::seqNo, IndexColumn::id))
            .map(column -> indexColumnClause(column, index.type(), context))
            .collect(Collectors.joining(", ")))
        .append(")");

    if (index.type() == IndexType.BTREE) {
      ddl.append(" USING ").append(index.type().name());
    }

    ddl.append(";");
    return ddl.toString();
  }

  private static String indexColumnClause(IndexColumn column, IndexType indexType,
      DdlContext context) {
    String clause = quoteColumn(context, column.columnId());
    if (indexType == IndexType.BTREE && column.sortDirection() != null) {
      clause += " " + column.sortDirection().name();
    }
    return clause;
  }

  private List<String> foreignKeyStatements(TableSnapshot snapshot,
      DdlContext context) {
    Table table = requireTable(snapshot);
    return snapshot.relationships().stream()
        .map(MySqlDdlGenerator::requireRelationship)
        .filter(relationship -> table.id().equals(
            relationship.relationship().fkTableId()))
        .sorted(comparingNullableStrings(
            relationship -> relationship.relationship().name(),
            relationship -> relationship.relationship().id()))
        .map(relationship -> foreignKeyStatement(table, relationship, context))
        .toList();
  }

  private String foreignKeyStatement(Table table,
      RelationshipSnapshot snapshot,
      DdlContext context) {
    Relationship relationship = snapshot.relationship();
    requireIdentifier(relationship.name(), "Foreign key name");
    Table pkTable = context.tableById().get(relationship.pkTableId());
    if (pkTable == null) {
      throw new DomainException(DdlErrorCode.TABLE_NOT_FOUND,
          "Referenced table not found: " + relationship.pkTableId());
    }
    if (snapshot.columns().isEmpty()) {
      throw invalid("Foreign key '%s' must contain at least one column"
          .formatted(relationship.name()));
    }

    List<RelationshipColumn> columns = snapshot.columns().stream()
        .sorted(comparingSeqNo(RelationshipColumn::seqNo,
            RelationshipColumn::id))
        .toList();
    Optional<String> onDelete = sanitizeReferentialAction(
        relationship.onDelete());
    Optional<String> onUpdate = sanitizeReferentialAction(
        relationship.onUpdate());
    validateForeignKeyColumns(table, pkTable, columns, onDelete, onUpdate,
        context);

    String fkColumns = columns.stream()
        .map(column -> quoteColumn(context, table.id(), column.fkColumnId()))
        .collect(Collectors.joining(", "));
    String pkColumns = columns.stream()
        .map(column -> quoteColumn(context, pkTable.id(),
            column.pkColumnId()))
        .collect(Collectors.joining(", "));

    StringBuilder ddl = new StringBuilder();
    ddl.append("ALTER TABLE ")
        .append(quoteIdentifier(table.name()))
        .append(" ADD CONSTRAINT ")
        .append(quoteIdentifier(relationship.name()))
        .append(" FOREIGN KEY (")
        .append(fkColumns)
        .append(") REFERENCES ")
        .append(quoteIdentifier(pkTable.name()))
        .append(" (")
        .append(pkColumns)
        .append(")");

    onDelete.ifPresent(action -> ddl.append(" ON DELETE ").append(action));
    onUpdate.ifPresent(action -> ddl.append(" ON UPDATE ").append(action));

    ddl.append(";");
    return ddl.toString();
  }

  private static void validateColumnDefinition(Column column, String dataType,
      ColumnRules columnRules) {
    validateTypeArguments(dataType, column.typeArguments());

    if ((hasText(column.charset()) || hasText(column.collation()))
        && !CHARACTER_STRING_TYPES.contains(dataType)) {
      throw invalid("Charset or collation is only allowed for character string type: "
          + dataType);
    }

    if (!column.autoIncrement()) {
      return;
    }
    if (!INTEGER_TYPES.contains(dataType)) {
      throw invalid("AUTO_INCREMENT is only allowed for integer types: "
          + dataType);
    }
    if (!columnRules.leftmostKeyColumnIds().contains(column.id())) {
      throw invalid("AUTO_INCREMENT column '%s' must be the first column of a key"
          .formatted(column.name()));
    }
    if (columnRules.defaultExpressionsByColumnId().containsKey(column.id())) {
      throw invalid("AUTO_INCREMENT column '%s' cannot have a DEFAULT value"
          .formatted(column.name()));
    }
  }

  private static void validateTypeArguments(String dataType,
      ColumnTypeArguments arguments) {
    if (arguments == null || arguments.isEmpty()) {
      if (REQUIRED_LENGTH_TYPES.contains(dataType)) {
        throw invalid("Length is required for MySQL type: " + dataType);
      }
      if (VALUE_LIST_TYPES.contains(dataType)) {
        throw invalid("Values are required for MySQL " + dataType + " type");
      }
      return;
    }

    if (arguments.hasValues()) {
      validateValueListArguments(dataType, arguments.values());
      return;
    }

    if (arguments.hasLength()) {
      validateLengthArgument(dataType, arguments.length());
      return;
    }

    if (arguments.hasPrecisionScale()) {
      if (!PRECISION_SCALE_TYPES.contains(dataType)) {
        throw invalid("Precision/scale is not allowed for MySQL type: "
            + dataType);
      }
      if (arguments.scale() > arguments.precision()) {
        throw invalid("Scale must not be greater than precision");
      }
      return;
    }
  }

  private static void validateLengthArgument(String dataType, int length) {
    if (!REQUIRED_LENGTH_TYPES.contains(dataType)
        && !OPTIONAL_LENGTH_TYPES.contains(dataType)) {
      throw invalid("Length is not allowed for MySQL type: " + dataType);
    }

    if ("BIT".equals(dataType) && length > MAX_BIT_LENGTH) {
      throw invalid("BIT length must be at most " + MAX_BIT_LENGTH);
    }
    if (Set.of("CHAR", "BINARY").contains(dataType)
        && length > MAX_CHAR_LENGTH) {
      throw invalid(dataType + " length must be at most " + MAX_CHAR_LENGTH);
    }
    if (REQUIRED_LENGTH_TYPES.contains(dataType)
        && length > MAX_VARCHAR_LENGTH) {
      throw invalid(dataType + " length must be at most "
          + MAX_VARCHAR_LENGTH);
    }
    if (TEMPORAL_FSP_TYPES.contains(dataType)
        && length > MAX_TEMPORAL_FSP) {
      throw invalid(dataType + " fractional seconds precision must be at most "
          + MAX_TEMPORAL_FSP);
    }
    if ("YEAR".equals(dataType) && length != 4) {
      throw invalid("YEAR length must be 4");
    }
    if (INTEGER_TYPES.contains(dataType)
        && length > MAX_INTEGER_DISPLAY_WIDTH) {
      throw invalid(dataType + " display width must be at most "
          + MAX_INTEGER_DISPLAY_WIDTH);
    }
    if ("FLOAT".equals(dataType) && length > MAX_FLOAT_PRECISION_BITS) {
      throw invalid("FLOAT precision must be at most "
          + MAX_FLOAT_PRECISION_BITS);
    }
  }

  private static void validateValueListArguments(String dataType,
      List<String> values) {
    if (!VALUE_LIST_TYPES.contains(dataType)) {
      throw invalid("Values are only allowed for MySQL ENUM/SET types");
    }
    int maxValues = "SET".equals(dataType) ? MAX_SET_VALUES : MAX_ENUM_VALUES;
    if (values.size() > maxValues) {
      throw invalid(dataType + " values must contain at most "
          + maxValues + " items");
    }
    for (String value : values) {
      if (value.length() > MAX_ENUM_SET_VALUE_LENGTH) {
        throw invalid(dataType + " value must be at most "
            + MAX_ENUM_SET_VALUE_LENGTH + " characters");
      }
      escapeString(value);
    }
  }

  private static void validateKeyColumns(Table table, List<String> columnIds,
      DdlContext context, String keyName) {
    for (String columnId : columnIds) {
      Column column = columnInTable(context, table.id(), columnId);
      String dataType = sanitizeDataType(column.dataType());
      if (TEXT_OR_BLOB_TYPES.contains(dataType) || "JSON".equals(dataType)) {
        throw invalid(keyName + " cannot use MySQL " + dataType
            + " column without a prefix length");
      }
      if (SPATIAL_TYPES.contains(dataType)) {
        throw invalid(keyName + " cannot be defined on a MySQL spatial column");
      }
    }
  }

  private static void validateIndexColumns(TableSnapshot tableSnapshot,
      IndexSnapshot snapshot,
      DdlContext context) {
    Table table = requireTable(tableSnapshot);
    IndexType type = snapshot.index().type();
    List<IndexColumn> columns = snapshot.columns().stream()
        .sorted(comparingSeqNo(IndexColumn::seqNo, IndexColumn::id))
        .toList();

    if (type == IndexType.SPATIAL) {
      validateSpatialIndex(tableSnapshot, columns, context);
      return;
    }

    for (IndexColumn indexColumn : columns) {
      Column column = columnInTable(context, table.id(),
          indexColumn.columnId());
      String dataType = sanitizeDataType(column.dataType());
      if (type == IndexType.FULLTEXT) {
        if (!FULLTEXT_TYPES.contains(dataType)) {
          throw invalid("FULLTEXT index can only use MySQL character string columns");
        }
        continue;
      }
      if (TEXT_OR_BLOB_TYPES.contains(dataType) || "JSON".equals(dataType)) {
        throw invalid("Index '%s' cannot use MySQL %s column without a prefix length"
            .formatted(snapshot.index().name(), dataType));
      }
    }
  }

  private static void validateSpatialIndex(TableSnapshot tableSnapshot,
      List<IndexColumn> columns,
      DdlContext context) {
    if (columns.size() != 1) {
      throw invalid("SPATIAL index must contain exactly one column");
    }
    Table table = requireTable(tableSnapshot);
    IndexColumn indexColumn = columns.getFirst();
    Column column = columnInTable(context, table.id(), indexColumn.columnId());
    String dataType = sanitizeDataType(column.dataType());
    if (!SPATIAL_TYPES.contains(dataType)) {
      throw invalid("SPATIAL index can only use MySQL spatial columns");
    }
    if (!notNullColumnIds(tableSnapshot).contains(column.id())) {
      throw invalid("SPATIAL index column '%s' must be NOT NULL"
          .formatted(column.name()));
    }
  }

  private static void validateForeignKeyColumns(Table fkTable, Table pkTable,
      List<RelationshipColumn> columns,
      Optional<String> onDelete,
      Optional<String> onUpdate,
      DdlContext context) {
    TableSnapshot fkSnapshot = context.tableSnapshotById().get(fkTable.id());
    TableSnapshot pkSnapshot = context.tableSnapshotById().get(pkTable.id());
    if (fkSnapshot == null || pkSnapshot == null) {
      throw invalid("Foreign key table snapshot is missing");
    }

    List<String> pkColumnIds = columns.stream()
        .map(RelationshipColumn::pkColumnId)
        .toList();
    if (!hasIndexPrefix(pkSnapshot, pkColumnIds)) {
      throw invalid("Referenced columns must be indexed in referenced table");
    }

    Set<String> fkNotNullColumnIds = notNullColumnIds(fkSnapshot);
    boolean usesSetNull = "SET NULL".equals(onDelete.orElse(null))
        || "SET NULL".equals(onUpdate.orElse(null));

    for (RelationshipColumn relationshipColumn : columns) {
      Column fkColumn = columnInTable(context, fkTable.id(),
          relationshipColumn.fkColumnId());
      Column pkColumn = columnInTable(context, pkTable.id(),
          relationshipColumn.pkColumnId());
      if (usesSetNull && fkNotNullColumnIds.contains(fkColumn.id())) {
        throw invalid("SET NULL referential action cannot target NOT NULL column: "
            + fkColumn.name());
      }
      validateForeignKeyColumnCompatibility(fkColumn, fkTable, pkColumn,
          pkTable);
    }
  }

  private static void validateForeignKeyColumnCompatibility(Column fkColumn,
      Table fkTable,
      Column pkColumn,
      Table pkTable) {
    String fkType = canonicalDataType(sanitizeDataType(fkColumn.dataType()));
    String pkType = canonicalDataType(sanitizeDataType(pkColumn.dataType()));

    if (TEXT_OR_BLOB_TYPES.contains(fkType) || TEXT_OR_BLOB_TYPES.contains(pkType)
        || SPATIAL_TYPES.contains(fkType) || SPATIAL_TYPES.contains(pkType)
        || "JSON".equals(fkType) || "JSON".equals(pkType)) {
      throw invalid("Foreign key columns cannot use MySQL TEXT, BLOB, JSON, or spatial types");
    }

    if (CHARACTER_STRING_TYPES.contains(fkType)
        || CHARACTER_STRING_TYPES.contains(pkType)) {
      if (!CHARACTER_STRING_TYPES.contains(fkType)
          || !CHARACTER_STRING_TYPES.contains(pkType)) {
        throw invalid("Foreign key columns must have similar MySQL data types");
      }
      if (!normalizedNullable(effectiveCharset(fkColumn, fkTable))
          .equals(normalizedNullable(effectiveCharset(pkColumn, pkTable)))
          || !normalizedNullable(effectiveCollation(fkColumn, fkTable))
              .equals(normalizedNullable(effectiveCollation(pkColumn, pkTable)))) {
        throw invalid("Foreign key character columns must use the same charset and collation");
      }
      return;
    }

    if (BINARY_STRING_TYPES.contains(fkType)
        || BINARY_STRING_TYPES.contains(pkType)) {
      if (!BINARY_STRING_TYPES.contains(fkType)
          || !BINARY_STRING_TYPES.contains(pkType)) {
        throw invalid("Foreign key columns must have similar MySQL data types");
      }
      return;
    }

    if (!fkType.equals(pkType)) {
      throw invalid("Foreign key columns must have similar MySQL data types");
    }

    if (PRECISION_SCALE_TYPES.contains(fkType)
        && !samePrecisionScale(fkColumn.typeArguments(),
            pkColumn.typeArguments())) {
      throw invalid("Foreign key fixed precision columns must have the same precision and scale");
    }
  }

  private static boolean hasIndexPrefix(TableSnapshot snapshot,
      List<String> columnIds) {
    for (ConstraintKind kind : List.of(
        ConstraintKind.PRIMARY_KEY, ConstraintKind.UNIQUE)) {
      boolean found = constraintsOf(snapshot, kind).stream()
          .map(constraint -> requireConstraintColumns(
              constraint, constraint.constraint().name()).stream()
              .sorted(comparingSeqNo(ConstraintColumn::seqNo,
                  ConstraintColumn::id))
              .map(ConstraintColumn::columnId)
              .toList())
          .anyMatch(indexColumnIds -> startsWith(indexColumnIds, columnIds));
      if (found) {
        return true;
      }
    }

    return snapshot.indexes().stream()
        .map(MySqlDdlGenerator::requireIndex)
        .filter(index -> index.index().type() == IndexType.BTREE)
        .map(index -> index.columns().stream()
            .sorted(comparingSeqNo(IndexColumn::seqNo, IndexColumn::id))
            .map(IndexColumn::columnId)
            .toList())
        .anyMatch(indexColumnIds -> startsWith(indexColumnIds, columnIds));
  }

  private static boolean startsWith(List<String> values,
      List<String> prefix) {
    if (values.size() < prefix.size()) {
      return false;
    }
    for (int i = 0; i < prefix.size(); i++) {
      if (!values.get(i).equals(prefix.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static Set<String> notNullColumnIds(TableSnapshot snapshot) {
    Set<String> columnIds = new HashSet<>();
    columnIds.addAll(ColumnRules.collectColumnIds(
        constraintsOf(snapshot, ConstraintKind.PRIMARY_KEY)));
    columnIds.addAll(snapshot.constraints().stream()
        .filter(constraint -> requireConstraint(constraint).kind() == ConstraintKind.NOT_NULL)
        .flatMap(constraint -> requireConstraintColumns(
            constraint, constraint.constraint().name()).stream())
        .map(ConstraintColumn::columnId)
        .collect(Collectors.toSet()));
    return columnIds;
  }

  private static String canonicalDataType(String dataType) {
    return switch (dataType) {
    case "INTEGER" -> "INT";
    case "BOOL", "BOOLEAN" -> "TINYINT";
    case "NUMERIC" -> "DECIMAL";
    case "REAL" -> "DOUBLE";
    default -> dataType;
    };
  }

  private static boolean samePrecisionScale(ColumnTypeArguments left,
      ColumnTypeArguments right) {
    return numberOrNull(left == null ? null : left.precision())
        .equals(numberOrNull(right == null ? null : right.precision()))
        && numberOrNull(left == null ? null : left.scale())
            .equals(numberOrNull(right == null ? null : right.scale()));
  }

  private static Integer numberOrNull(Integer value) {
    return value;
  }

  private static String effectiveCharset(Column column, Table table) {
    return hasText(column.charset()) ? column.charset() : table.charset();
  }

  private static String effectiveCollation(Column column, Table table) {
    return hasText(column.collation()) ? column.collation() : table.collation();
  }

  private static String normalizedNullable(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static void requireSnapshot(DdlSchemaSnapshot snapshot) {
    if (snapshot == null || snapshot.schema() == null) {
      throw invalid("DDL schema snapshot must not be null");
    }
    requireIdentifier(snapshot.schema().name(), "Schema name");
    requireNonBlank(snapshot.schema().dbVendorName(), "Database vendor name");
  }

  private static void requireMysqlCompatible(String dbVendorName) {
    String normalized = dbVendorName.trim().toLowerCase(Locale.ROOT);
    if (!"mysql".equals(normalized) && !"mariadb".equals(normalized)) {
      throw new DomainException(DdlErrorCode.UNSUPPORTED_VENDOR,
          "Unsupported DDL export vendor: " + dbVendorName);
    }
  }

  private static Table requireTable(TableSnapshot snapshot) {
    if (snapshot == null || snapshot.table() == null) {
      throw invalid("Table snapshot must not be null");
    }
    Table table = snapshot.table();
    requireIdentifier(table.id(), "Table id");
    requireIdentifier(table.name(), "Table name");
    return table;
  }

  private static void requireColumn(Column column) {
    if (column == null) {
      throw invalid("Column snapshot must not be null");
    }
    requireIdentifier(column.id(), "Column id");
    requireIdentifier(column.tableId(), "Column table id");
    requireIdentifier(column.name(), "Column name");
    requireNonBlank(column.dataType(), "Column data type");
  }

  private static Constraint requireConstraint(ConstraintSnapshot snapshot) {
    if (snapshot == null || snapshot.constraint() == null) {
      throw invalid("Constraint snapshot must not be null");
    }
    Constraint constraint = snapshot.constraint();
    requireIdentifier(constraint.id(), "Constraint id");
    if (constraint.kind() == null) {
      throw invalid("Constraint kind must not be null");
    }
    return constraint;
  }

  private static IndexSnapshot requireIndex(IndexSnapshot snapshot) {
    if (snapshot == null || snapshot.index() == null) {
      throw invalid("Index snapshot must not be null");
    }
    requireIdentifier(snapshot.index().id(), "Index id");
    return snapshot;
  }

  private static RelationshipSnapshot requireRelationship(
      RelationshipSnapshot snapshot) {
    if (snapshot == null || snapshot.relationship() == null) {
      throw invalid("Relationship snapshot must not be null");
    }
    Relationship relationship = snapshot.relationship();
    requireIdentifier(relationship.id(), "Relationship id");
    requireIdentifier(relationship.pkTableId(), "Relationship pkTableId");
    requireIdentifier(relationship.fkTableId(), "Relationship fkTableId");
    return snapshot;
  }

  private static List<ConstraintColumn> requireConstraintColumns(
      ConstraintSnapshot snapshot,
      String constraintName) {
    if (snapshot.columns().isEmpty()) {
      throw invalid("Constraint '%s' must contain at least one column"
          .formatted(constraintName));
    }
    return snapshot.columns();
  }

  private static List<TableSnapshot> normalizeTables(
      List<TableSnapshot> tables) {
    if (tables == null || tables.isEmpty()) {
      return List.of();
    }
    return tables.stream()
        .map(snapshot -> {
          requireTable(snapshot);
          return snapshot;
        })
        .sorted(comparingNullableStrings(
            snapshot -> snapshot.table().name(),
            snapshot -> snapshot.table().id()))
        .toList();
  }

  private static List<Column> sortColumns(List<Column> columns) {
    return columns.stream()
        .peek(MySqlDdlGenerator::requireColumn)
        .sorted(comparingSeqNo(Column::seqNo, Column::id))
        .toList();
  }

  private static List<ConstraintSnapshot> constraintsOf(
      TableSnapshot snapshot,
      ConstraintKind kind) {
    return snapshot.constraints().stream()
        .filter(constraint -> requireConstraint(constraint).kind() == kind)
        .toList();
  }

  private static String quoteColumn(DdlContext context, String columnId) {
    Column column = context.columnById().get(columnId);
    if (column == null) {
      throw new DomainException(DdlErrorCode.COLUMN_NOT_FOUND,
          "Column not found: " + columnId);
    }
    return quoteIdentifier(column.name());
  }

  private static String quoteColumn(DdlContext context, String tableId,
      String columnId) {
    return quoteIdentifier(columnInTable(context, tableId, columnId).name());
  }

  private static Column columnInTable(DdlContext context, String tableId,
      String columnId) {
    Column column = context.columnById().get(columnId);
    if (column == null) {
      throw new DomainException(DdlErrorCode.COLUMN_NOT_FOUND,
          "Column not found: " + columnId);
    }
    if (!tableId.equals(column.tableId())) {
      throw invalid("Column '%s' does not belong to table '%s'"
          .formatted(columnId, tableId));
    }
    return column;
  }

  private static String quoteIdentifier(String identifier) {
    String sanitized = requireIdentifier(identifier, "Identifier");
    return "`" + sanitized.replace("`", "``") + "`";
  }

  private static String requireIdentifier(String value, String name) {
    String identifier = requireNonBlank(value, name);
    if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
      throw invalid(name + " must be at most " + MAX_IDENTIFIER_LENGTH
          + " characters");
    }
    if (identifier.endsWith(" ")) {
      throw invalid(name + " must not end with a space");
    }
    for (int i = 0; i < identifier.length();) {
      int codePoint = identifier.codePointAt(i);
      if (codePoint == 0 || codePoint > 0xFFFF
          || Character.isISOControl(codePoint)) {
        throw invalid(name + " contains an invalid control character");
      }
      i += Character.charCount(codePoint);
    }
    return identifier;
  }

  private static String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw invalid(name + " must not be blank");
    }
    return value;
  }

  private static String sanitizeDataType(String dataType) {
    String normalized = requireNonBlank(dataType, "Column data type")
        .trim()
        .toUpperCase(Locale.ROOT);
    char first = normalized.charAt(0);
    if (first < 'A' || first > 'Z') {
      throw invalid("Column data type must start with a letter");
    }
    for (int i = 1; i < normalized.length(); i++) {
      char c = normalized.charAt(i);
      boolean valid = (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || c == '_'
          || c == ' ';
      if (!valid) {
        throw invalid("Column data type contains an invalid character");
      }
    }
    if (!SUPPORTED_DATA_TYPES.contains(normalized)) {
      throw invalid("Unsupported MySQL data type: " + dataType);
    }
    return normalized;
  }

  private static Optional<String> sanitizeOptionalIdentifier(String value,
      String name) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    String trimmed = value.trim();
    char first = trimmed.charAt(0);
    boolean validFirst = (first >= 'a' && first <= 'z')
        || (first >= 'A' && first <= 'Z');
    if (!validFirst) {
      throw invalid(name + " has an invalid format");
    }
    for (int i = 1; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      boolean valid = (c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || c == '_';
      if (!valid) {
        throw invalid(name + " has an invalid format");
      }
    }
    return Optional.of(trimmed);
  }

  private static Optional<String> sanitizeReferentialAction(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    if (!VALID_REFERENTIAL_ACTIONS.contains(normalized)) {
      throw invalid("Unsupported referential action: " + value);
    }
    return Optional.of(normalized);
  }

  private static String sanitizeExpression(String expression, String name) {
    String value = requireNonBlank(expression, name).trim();
    if (value.contains(";") || value.contains("--") || value.contains("/*")
        || value.contains("*/")) {
      throw invalid(name + " contains a statement separator or comment");
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\0' || c == '\r' || c == '\n') {
        throw invalid(name + " contains an invalid control character");
      }
    }
    return value;
  }

  private static String escapeString(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
      case '\0' -> escaped.append("\\0");
      case '\b' -> escaped.append("\\b");
      case '\n' -> escaped.append("\\n");
      case '\r' -> escaped.append("\\r");
      case '\t' -> escaped.append("\\t");
      case 26 -> escaped.append("\\Z");
      case '\\' -> escaped.append("\\\\");
      case '\'' -> escaped.append("''");
      default -> {
        if (Character.isISOControl(c)) {
          throw invalid("String literal contains an invalid control character");
        }
        escaped.append(c);
      }
      }
    }
    return escaped.toString();
  }

  private static String escapeComment(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("--", "- -")
        .replace("\r", " ")
        .replace("\n", " ");
  }

  private static <T> Comparator<T> comparingNullableStrings(
      Function<T, String> primary,
      Function<T, String> secondary) {
    return Comparator.comparing((T value) -> nullToEmpty(primary.apply(value)))
        .thenComparing(value -> nullToEmpty(secondary.apply(value)));
  }

  private static <T> Comparator<T> comparingSeqNo(
      Function<T, Integer> seqNo,
      Function<T, String> id) {
    return Comparator.comparing(seqNo::apply)
        .thenComparing(value -> nullToEmpty(id.apply(value)));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static DomainException invalid(String message) {
    return new DomainException(DdlErrorCode.INVALID_VALUE, message);
  }

  private record ColumnRules(
      Set<String> notNullColumnIds,
      Map<String, String> defaultExpressionsByColumnId,
      Set<String> leftmostKeyColumnIds,
      Set<String> autoIncrementColumnIds) {

    private static ColumnRules from(TableSnapshot snapshot,
        DdlContext context) {
      Set<String> primaryKeyColumnIds = collectColumnIds(
          constraintsOf(snapshot, ConstraintKind.PRIMARY_KEY));
      Set<String> notNullColumnIds = snapshot.constraints().stream()
          .filter(constraint -> requireConstraint(constraint).kind() == ConstraintKind.NOT_NULL)
          .flatMap(constraint -> requireConstraintColumns(
              constraint, constraint.constraint().name()).stream())
          .map(ConstraintColumn::columnId)
          .collect(Collectors.toSet());
      notNullColumnIds.addAll(primaryKeyColumnIds);

      Map<String, String> defaultExpressions = collectDefaultExpressions(
          snapshot);
      Set<String> leftmostKeyColumnIds = collectLeftmostKeyColumnIds(snapshot);
      Set<String> autoIncrementColumnIds = collectAutoIncrementColumnIds(
          snapshot);

      for (String columnId : notNullColumnIds) {
        quoteColumn(context, snapshot.table().id(), columnId);
      }
      for (String columnId : defaultExpressions.keySet()) {
        quoteColumn(context, snapshot.table().id(), columnId);
      }
      for (String columnId : leftmostKeyColumnIds) {
        quoteColumn(context, snapshot.table().id(), columnId);
      }
      for (String columnId : autoIncrementColumnIds) {
        quoteColumn(context, snapshot.table().id(), columnId);
      }

      return new ColumnRules(notNullColumnIds, defaultExpressions,
          leftmostKeyColumnIds, autoIncrementColumnIds);
    }

    private static Set<String> collectAutoIncrementColumnIds(
        TableSnapshot snapshot) {
      List<String> autoIncrementColumnIds = snapshot.columns().stream()
          .peek(MySqlDdlGenerator::requireColumn)
          .filter(Column::autoIncrement)
          .map(Column::id)
          .toList();
      if (autoIncrementColumnIds.size() > 1) {
        throw invalid("Only one AUTO_INCREMENT column is allowed per table");
      }
      return Set.copyOf(autoIncrementColumnIds);
    }

    private static Set<String> collectLeftmostKeyColumnIds(
        TableSnapshot snapshot) {
      Set<String> columnIds = new HashSet<>();
      for (ConstraintKind kind : List.of(
          ConstraintKind.PRIMARY_KEY, ConstraintKind.UNIQUE)) {
        for (ConstraintSnapshot constraint : constraintsOf(snapshot, kind)) {
          firstConstraintColumnId(constraint).ifPresent(columnIds::add);
        }
      }
      snapshot.indexes().stream()
          .map(MySqlDdlGenerator::requireIndex)
          .filter(index -> index.index().type() == IndexType.BTREE)
          .map(ColumnRules::firstIndexColumnId)
          .flatMap(Optional::stream)
          .forEach(columnIds::add);
      return columnIds;
    }

    private static Optional<String> firstConstraintColumnId(
        ConstraintSnapshot constraint) {
      return requireConstraintColumns(
          constraint, constraint.constraint().name()).stream()
          .sorted(comparingSeqNo(ConstraintColumn::seqNo,
              ConstraintColumn::id))
          .map(ConstraintColumn::columnId)
          .findFirst();
    }

    private static Optional<String> firstIndexColumnId(IndexSnapshot index) {
      if (index.columns().isEmpty()) {
        throw invalid("Index '%s' must contain at least one column"
            .formatted(index.index().name()));
      }
      return index.columns().stream()
          .sorted(comparingSeqNo(IndexColumn::seqNo, IndexColumn::id))
          .map(IndexColumn::columnId)
          .findFirst();
    }

    private static Map<String, String> collectDefaultExpressions(
        TableSnapshot snapshot) {
      Map<String, String> defaults = new LinkedHashMap<>();
      for (ConstraintSnapshot constraint : constraintsOf(
          snapshot, ConstraintKind.DEFAULT)) {
        Constraint definition = requireConstraint(constraint);
        List<ConstraintColumn> columns = requireConstraintColumns(
            constraint, definition.name());
        if (columns.size() != 1) {
          throw invalid("DEFAULT constraint '%s' must target exactly one column"
              .formatted(definition.name()));
        }
        String previous = defaults.put(
            columns.getFirst().columnId(),
            requireNonBlank(definition.defaultExpr(),
                "Default expression"));
        if (previous != null) {
          throw invalid("Column '%s' has multiple DEFAULT constraints"
              .formatted(columns.getFirst().columnId()));
        }
      }
      return defaults;
    }

    private static Set<String> collectColumnIds(
        List<ConstraintSnapshot> constraints) {
      return constraints.stream()
          .flatMap(constraint -> requireConstraintColumns(
              constraint, constraint.constraint().name()).stream())
          .map(ConstraintColumn::columnId)
          .collect(Collectors.toSet());
    }

  }

  private record DdlContext(
      Map<String, Table> tableById,
      Map<String, TableSnapshot> tableSnapshotById,
      Map<String, Column> columnById) {

    private static DdlContext from(List<TableSnapshot> tables) {
      Map<String, Table> tableById = new HashMap<>();
      Map<String, TableSnapshot> tableSnapshotById = new HashMap<>();
      Map<String, Column> columnById = new HashMap<>();
      Set<String> tableNames = new HashSet<>();
      for (TableSnapshot snapshot : tables) {
        Table table = requireTable(snapshot);
        if (tableById.put(table.id(), table) != null) {
          throw invalid("Duplicate table id: " + table.id());
        }
        if (tableSnapshotById.put(table.id(), snapshot) != null) {
          throw invalid("Duplicate table id: " + table.id());
        }
        if (!tableNames.add(table.name().toLowerCase(Locale.ROOT))) {
          throw invalid("Duplicate table name: " + table.name());
        }
        Set<String> columnNames = new HashSet<>();
        for (Column column : snapshot.columns()) {
          requireColumn(column);
          if (!table.id().equals(column.tableId())) {
            throw invalid("Column '%s' does not belong to table '%s'"
                .formatted(column.id(), table.id()));
          }
          if (columnById.put(column.id(), column) != null) {
            throw invalid("Duplicate column id: " + column.id());
          }
          if (!columnNames.add(column.name().toLowerCase(Locale.ROOT))) {
            throw invalid("Duplicate column name in table '%s': %s"
                .formatted(table.name(), column.name()));
          }
        }
      }
      return new DdlContext(
          Map.copyOf(tableById),
          Map.copyOf(tableSnapshotById),
          Map.copyOf(columnById));
    }

  }

}
