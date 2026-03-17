package com.schemafy.core.erd.column.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;

@Component
class ColumnMapper {

  private final JsonCodec jsonCodec;

  ColumnMapper(JsonCodec jsonCodec) {
    this.jsonCodec = jsonCodec;
  }

  ColumnEntity toEntity(Column column) {
    return new ColumnEntity(
        column.id(),
        column.tableId(),
        column.name(),
        column.dataType(),
        toTypeArgumentsJson(column.typeArguments()),
        column.seqNo(),
        column.autoIncrement(),
        column.charset(),
        column.collation(),
        column.comment());
  }

  Column toDomain(ColumnEntity entity) {
    return new Column(
        entity.getId(),
        entity.getTableId(),
        entity.getName(),
        entity.getDataType(),
        toTypeArguments(entity.getTypeArguments()),
        entity.getSeqNo(),
        Boolean.TRUE.equals(entity.getAutoIncrement()),
        entity.getCharset(),
        entity.getCollation(),
        entity.getComment());
  }

  String toTypeArgumentsJson(ColumnTypeArguments typeArguments) {
    if (typeArguments == null) {
      return null;
    }
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    if (typeArguments.length() != null) {
      node.put("length", typeArguments.length());
    }
    if (typeArguments.precision() != null) {
      node.put("precision", typeArguments.precision());
    }
    if (typeArguments.scale() != null) {
      node.put("scale", typeArguments.scale());
    }
    if (typeArguments.values() != null) {
      node.putPOJO("values", typeArguments.values());
    }
    return jsonCodec.canonicalize(node);
  }

  private ColumnTypeArguments toTypeArguments(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return null;
    }

    JsonNode node = jsonCodec.parsePersistedNode(rawJson);
    Integer length = intOrNull(node.get("length"));
    Integer precision = intOrNull(node.get("precision"));
    Integer scale = intOrNull(node.get("scale"));
    List<String> values = valuesOrNull(node.get("values"));

    return ColumnTypeArguments.from(length, precision, scale, values);
  }

  private static Integer intOrNull(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isIntegralNumber()) {
      throw new IllegalArgumentException("JSON field must be an integer");
    }
    return node.intValue();
  }

  private List<String> valuesOrNull(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isArray()) {
      throw new IllegalArgumentException("JSON field must be an array");
    }
    String[] values = jsonCodec.parse(jsonCodec.canonicalize(node),
        String[].class);
    return List.of(values);
  }

}
