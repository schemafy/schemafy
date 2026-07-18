package com.schemafy.core.erd.mermaid.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Column;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.ConstraintSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Relationship;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Table;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.TableSnapshot;
import com.schemafy.core.erd.mermaid.application.port.in.GenerateSchemaMermaidCommand;
import com.schemafy.core.erd.mermaid.application.port.in.GenerateSchemaMermaidUseCase;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import reactor.core.publisher.Mono;

@Service
public class GenerateSchemaMermaidService implements
    GenerateSchemaMermaidUseCase {

  private static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile(
      "^[A-Za-z_][A-Za-z0-9_]*$");
  private static final Pattern ATTRIBUTE_TYPE_PATTERN = Pattern.compile(
      "^[A-Za-z][A-Za-z0-9_-]*$");
  private static final Comparator<TableSnapshot> TABLE_COMPARATOR = Comparator
      .comparing((TableSnapshot snapshot) -> snapshot.table().name())
      .thenComparing(snapshot -> snapshot.table().id());
  private static final Comparator<Column> COLUMN_COMPARATOR = Comparator
      .comparingInt(Column::seqNo)
      .thenComparing(Column::id);
  private static final Comparator<RelationshipSnapshot> RELATIONSHIP_COMPARATOR = Comparator
      .comparing((RelationshipSnapshot snapshot) -> snapshot.relationship().name())
      .thenComparing(snapshot -> snapshot.relationship().id());

  @Override
  public Mono<String> generateSchemaMermaid(
      GenerateSchemaMermaidCommand command) {
    return Mono.fromSupplier(() -> generate(command.snapshot()));
  }

  private String generate(SchemaExportSnapshot snapshot) {
    requireSnapshot(snapshot);
    List<TableSnapshot> tables = snapshot.tables().stream()
        .peek(GenerateSchemaMermaidService::requireTableSnapshot)
        .sorted(TABLE_COMPARATOR)
        .toList();
    if (tables.isEmpty()) {
      return "erDiagram";
    }

    RenderContext context = RenderContext.from(tables);
    List<String> entities = tables.stream()
        .map(table -> generateEntity(table, context))
        .toList();
    List<String> relationships = generateRelationships(tables, context);

    StringBuilder mermaid = new StringBuilder("erDiagram\n");
    mermaid.append(String.join("\n", entities));
    if (!relationships.isEmpty()) {
      mermaid.append("\n\n");
      mermaid.append(String.join("\n", relationships));
    }
    return mermaid.toString();
  }

  private String generateEntity(TableSnapshot snapshot,
      RenderContext context) {
    Table table = snapshot.table();
    String declaration = "    " + context.entityId(table.id())
        + "[\"" + escapeQuotedText(table.name()) + "\"]";
    List<Column> columns = snapshot.columns().stream()
        .peek(column -> requireColumn(column, table.id()))
        .sorted(COLUMN_COMPARATOR)
        .toList();
    if (columns.isEmpty()) {
      return declaration;
    }

    Map<String, EnumSet<AttributeKey>> keysByColumnId = attributeKeys(
        snapshot, context);
    Set<String> notNullColumnIds = context.notNullColumnIds(table.id());
    String attributes = columns.stream()
        .map(column -> generateAttribute(column,
            keysByColumnId.getOrDefault(column.id(),
                EnumSet.noneOf(AttributeKey.class)),
            notNullColumnIds.contains(column.id())))
        .collect(Collectors.joining("\n"));
    return declaration + " {\n" + attributes + "\n    }";
  }

  private String generateAttribute(Column column,
      EnumSet<AttributeKey> keys,
      boolean notNull) {
    String name = column.name();
    if (!ATTRIBUTE_NAME_PATTERN.matcher(name).matches()) {
      throw new DomainException(ColumnErrorCode.NAME_INVALID,
          "Column name cannot be represented in Mermaid: " + name);
    }
    String dataType = requireText(column.dataType(),
        ColumnErrorCode.DATA_TYPE_INVALID,
        "Column data type must not be blank")
        .toUpperCase(Locale.ROOT);
    if (!ATTRIBUTE_TYPE_PATTERN.matcher(dataType).matches()) {
      throw new DomainException(ColumnErrorCode.DATA_TYPE_INVALID,
          "Column data type cannot be represented in Mermaid: " + dataType);
    }

    String suffix = keys.isEmpty()
        ? ""
        : " " + keys.stream()
            .map(Enum::name)
            .collect(Collectors.joining(", "));
    String formattedDataType = formatDataType(dataType,
        column.typeArguments());
    List<String> metadata = new ArrayList<>();
    if (!formattedDataType.equals(dataType)) {
      metadata.add(formattedDataType);
    }
    if (notNull) {
      metadata.add("NOT NULL");
    }
    if (column.autoIncrement()) {
      metadata.add("AUTO_INCREMENT");
    }
    String metadataComment = metadata.isEmpty()
        ? ""
        : " \"" + escapeQuotedText(String.join("; ", metadata)) + "\"";
    return "        " + dataType + " " + name + suffix
        + metadataComment;
  }

  private static String formatDataType(String dataType,
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
          .map(value -> "'" + escapeTypeArgumentValue(value) + "'")
          .collect(Collectors.joining(", "));
      return dataType + "(" + values + ")";
    }
    return dataType;
  }

  private static String escapeTypeArgumentValue(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("'", "''")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\t", "\\t");
  }

  private Map<String, EnumSet<AttributeKey>> attributeKeys(
      TableSnapshot snapshot,
      RenderContext context) {
    Map<String, EnumSet<AttributeKey>> keysByColumnId = new HashMap<>();
    for (ConstraintSnapshot constraintSnapshot : snapshot.constraints()) {
      if (constraintSnapshot == null || constraintSnapshot.constraint() == null
          || constraintSnapshot.constraint().kind() == null) {
        throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
            "Constraint snapshot must be complete");
      }
      AttributeKey key = switch (constraintSnapshot.constraint().kind()) {
      case PRIMARY_KEY -> AttributeKey.PK;
      case UNIQUE -> AttributeKey.UK;
      default -> null;
      };
      if (key == null) {
        continue;
      }
      for (var constraintColumn : constraintSnapshot.columns()) {
        context.requireColumn(snapshot.table().id(),
            constraintColumn.columnId());
        keysByColumnId.computeIfAbsent(constraintColumn.columnId(),
            ignored -> EnumSet.noneOf(AttributeKey.class)).add(key);
      }
    }

    for (RelationshipSnapshot relationshipSnapshot : ownedRelationships(
        snapshot)) {
      for (RelationshipColumn relationshipColumn : requireRelationshipColumns(
          relationshipSnapshot)) {
        context.requireColumn(snapshot.table().id(),
            relationshipColumn.fkColumnId());
        keysByColumnId.computeIfAbsent(relationshipColumn.fkColumnId(),
            ignored -> EnumSet.noneOf(AttributeKey.class))
            .add(AttributeKey.FK);
      }
    }
    return keysByColumnId;
  }

  private List<String> generateRelationships(List<TableSnapshot> tables,
      RenderContext context) {
    Set<String> renderedRelationshipIds = new HashSet<>();
    List<RelationshipSnapshot> relationships = tables.stream()
        .flatMap(table -> ownedRelationships(table).stream())
        .filter(snapshot -> renderedRelationshipIds.add(
            requireRelationship(snapshot).id()))
        .sorted(RELATIONSHIP_COMPARATOR)
        .toList();

    List<String> lines = new ArrayList<>(relationships.size());
    for (RelationshipSnapshot snapshot : relationships) {
      Relationship relationship = requireRelationship(snapshot);
      List<RelationshipColumn> columns = requireRelationshipColumns(snapshot);
      context.requireTable(relationship.pkTableId());
      context.requireTable(relationship.fkTableId());
      for (RelationshipColumn column : columns) {
        context.requireColumn(relationship.pkTableId(), column.pkColumnId());
        context.requireColumn(relationship.fkTableId(), column.fkColumnId());
      }

      String pkCardinality = areAllFkColumnsNotNull(
          relationship.fkTableId(), columns, context) ? "||" : "|o";
      String identification = switch (relationship.kind()) {
      case IDENTIFYING -> "--";
      case NON_IDENTIFYING -> "..";
      };
      String fkCardinality = switch (relationship.cardinality()) {
      case ONE_TO_ONE -> "o|";
      case ONE_TO_MANY -> "o{";
      };
      lines.add("    " + context.entityId(relationship.pkTableId())
          + " " + pkCardinality + identification + fkCardinality + " "
          + context.entityId(relationship.fkTableId())
          + " : \"" + escapeQuotedText(relationship.name()) + "\"");
    }
    return lines;
  }

  private static List<RelationshipSnapshot> ownedRelationships(
      TableSnapshot snapshot) {
    return snapshot.relationships().stream()
        .filter(relationshipSnapshot -> snapshot.table().id().equals(
            requireRelationship(relationshipSnapshot).fkTableId()))
        .toList();
  }

  private static boolean areAllFkColumnsNotNull(String fkTableId,
      List<RelationshipColumn> relationshipColumns,
      RenderContext context) {
    Set<String> notNullColumnIds = context.notNullColumnIds(fkTableId);
    return relationshipColumns.stream()
        .map(RelationshipColumn::fkColumnId)
        .allMatch(notNullColumnIds::contains);
  }

  private static Relationship requireRelationship(
      RelationshipSnapshot snapshot) {
    if (snapshot == null || snapshot.relationship() == null) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
          "Relationship snapshot must be complete");
    }
    Relationship relationship = snapshot.relationship();
    requireText(relationship.id(), RelationshipErrorCode.INVALID_VALUE,
        "Relationship id must not be blank");
    requireText(relationship.pkTableId(),
        RelationshipErrorCode.TARGET_TABLE_NOT_FOUND,
        "Relationship PK table id must not be blank");
    requireText(relationship.fkTableId(),
        RelationshipErrorCode.TARGET_TABLE_NOT_FOUND,
        "Relationship FK table id must not be blank");
    requireText(relationship.name(), RelationshipErrorCode.NAME_INVALID,
        "Relationship name must not be blank");
    if (relationship.kind() == null || relationship.cardinality() == null) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
          "Relationship kind and cardinality are required");
    }
    return relationship;
  }

  private static List<RelationshipColumn> requireRelationshipColumns(
      RelationshipSnapshot snapshot) {
    if (snapshot.columns().isEmpty()) {
      throw new DomainException(RelationshipErrorCode.EMPTY,
          "Relationship must contain at least one column mapping");
    }
    for (RelationshipColumn column : snapshot.columns()) {
      if (column == null) {
        throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
            "Relationship column must not be null");
      }
    }
    return snapshot.columns();
  }

  private static void requireSnapshot(SchemaExportSnapshot snapshot) {
    if (snapshot == null || snapshot.schema() == null) {
      throw new DomainException(SchemaErrorCode.INVALID_VALUE,
          "Schema export snapshot must be complete");
    }
  }

  private static void requireTableSnapshot(TableSnapshot snapshot) {
    if (snapshot == null || snapshot.table() == null) {
      throw new DomainException(TableErrorCode.INVALID_VALUE,
          "Table snapshot must be complete");
    }
    requireText(snapshot.table().id(), TableErrorCode.INVALID_VALUE,
        "Table id must not be blank");
    requireText(snapshot.table().name(), TableErrorCode.INVALID_VALUE,
        "Table name must not be blank");
  }

  private static void requireColumn(Column column, String tableId) {
    if (column == null) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE,
          "Column must not be null");
    }
    requireText(column.id(), ColumnErrorCode.INVALID_VALUE,
        "Column id must not be blank");
    requireText(column.name(), ColumnErrorCode.NAME_INVALID,
        "Column name must not be blank");
    if (!tableId.equals(column.tableId())) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE,
          "Column table id does not match its table snapshot");
    }
  }

  private static String requireText(String value,
      com.schemafy.core.common.exception.DomainErrorCode errorCode,
      String message) {
    if (value == null || value.isBlank()) {
      throw new DomainException(errorCode, message);
    }
    return value.trim();
  }

  private static String escapeQuotedText(String value) {
    return value.trim()
        .replace("\r\n", " ")
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace('\t', ' ')
        .replace("#", "#35;")
        .replace("\"", "#34;");
  }

  private enum AttributeKey {
    PK, FK, UK
  }

  private record RenderContext(
      Map<String, TableSnapshot> tablesById,
      Map<String, String> entityIdsByTableId,
      Map<String, Map<String, Column>> columnsByTableId,
      Map<String, Set<String>> notNullColumnIdsByTableId) {

    private static RenderContext from(List<TableSnapshot> tables) {
      Map<String, TableSnapshot> tablesById = new LinkedHashMap<>();
      Map<String, String> entityIdsByTableId = new LinkedHashMap<>();
      Map<String, Map<String, Column>> columnsByTableId = new HashMap<>();
      Map<String, Set<String>> notNullColumnIdsByTableId = new HashMap<>();

      for (int index = 0; index < tables.size(); index++) {
        TableSnapshot snapshot = tables.get(index);
        Table table = snapshot.table();
        if (tablesById.putIfAbsent(table.id(), snapshot) != null) {
          throw new DomainException(TableErrorCode.INVALID_VALUE,
              "Duplicate table id in schema export snapshot: " + table.id());
        }
        entityIdsByTableId.put(table.id(), "T" + (index + 1));

        Map<String, Column> columnsById = new HashMap<>();
        for (Column column : snapshot.columns()) {
          GenerateSchemaMermaidService.requireColumn(column, table.id());
          if (columnsById.putIfAbsent(column.id(), column) != null) {
            throw new DomainException(ColumnErrorCode.INVALID_VALUE,
                "Duplicate column id in table snapshot: " + column.id());
          }
        }
        columnsByTableId.put(table.id(), Map.copyOf(columnsById));
        notNullColumnIdsByTableId.put(table.id(),
            collectNotNullColumnIds(snapshot, columnsById));
      }

      return new RenderContext(
          Map.copyOf(tablesById),
          Map.copyOf(entityIdsByTableId),
          Map.copyOf(columnsByTableId),
          Map.copyOf(notNullColumnIdsByTableId));
    }

    private static Set<String> collectNotNullColumnIds(
        TableSnapshot snapshot,
        Map<String, Column> columnsById) {
      Set<String> notNullColumnIds = new HashSet<>();
      for (ConstraintSnapshot constraintSnapshot : snapshot.constraints()) {
        if (constraintSnapshot == null || constraintSnapshot.constraint() == null
            || constraintSnapshot.constraint().kind() == null) {
          throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
              "Constraint snapshot must be complete");
        }
        ConstraintKind kind = constraintSnapshot.constraint().kind();
        if (kind != ConstraintKind.PRIMARY_KEY
            && kind != ConstraintKind.NOT_NULL) {
          continue;
        }
        for (var constraintColumn : constraintSnapshot.columns()) {
          if (!columnsById.containsKey(constraintColumn.columnId())) {
            throw new DomainException(ColumnErrorCode.NOT_FOUND,
                "Constraint column not found: "
                    + constraintColumn.columnId());
          }
          notNullColumnIds.add(constraintColumn.columnId());
        }
      }
      return Set.copyOf(notNullColumnIds);
    }

    private String entityId(String tableId) {
      requireTable(tableId);
      return entityIdsByTableId.get(tableId);
    }

    private void requireTable(String tableId) {
      if (!tablesById.containsKey(tableId)) {
        throw new DomainException(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND,
            "Relationship table not found: " + tableId);
      }
    }

    private void requireColumn(String tableId, String columnId) {
      requireTable(tableId);
      if (!columnsByTableId.get(tableId).containsKey(columnId)) {
        throw new DomainException(RelationshipErrorCode.COLUMN_NOT_FOUND,
            "Relationship column not found: " + columnId);
      }
    }

    private Set<String> notNullColumnIds(String tableId) {
      requireTable(tableId);
      return notNullColumnIdsByTableId.get(tableId);
    }

  }

}
