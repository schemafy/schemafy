package com.schemafy.domain.erd.relationship.domain.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipCyclicReferenceException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipEmptyException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameInvalidException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipPositionInvalidException;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public final class RelationshipValidator {

  private static final int NAME_MIN_LENGTH = 1;
  private static final int NAME_MAX_LENGTH = 255;

  private RelationshipValidator() {}

  public static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new RelationshipNameInvalidException("Relationship name must not be blank");
    }
    String trimmed = name.trim();
    if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
      throw new RelationshipNameInvalidException(
          "Relationship name must be between %d and %d characters".formatted(
              NAME_MIN_LENGTH,
              NAME_MAX_LENGTH));
    }
  }

  public static void validateColumnsNotEmpty(List<RelationshipColumn> columns, String name) {
    if (columns == null || columns.isEmpty()) {
      throw new RelationshipEmptyException(
          "Relationship '%s' must have at least one column mapping".formatted(name));
    }
  }

  public static void validateSeqNoIntegrity(List<Integer> seqNos) {
    if (seqNos == null || seqNos.isEmpty()) {
      return;
    }
    Set<Integer> uniqueSeqNos = new HashSet<>();
    for (Integer seqNo : seqNos) {
      if (seqNo == null || seqNo < 0) {
        throw new RelationshipPositionInvalidException(
            "Relationship column position must be zero or positive");
      }
      if (!uniqueSeqNos.add(seqNo)) {
        throw new RelationshipPositionInvalidException(
            "Relationship column positions must be unique");
      }
    }
    for (int expected = 0; expected < uniqueSeqNos.size(); expected++) {
      if (!uniqueSeqNos.contains(expected)) {
        throw new RelationshipPositionInvalidException(
            "Relationship column positions must be contiguous starting from 0");
      }
    }
  }

  public static void validateColumnExistence(
      List<Column> fkColumns,
      List<Column> pkColumns,
      List<RelationshipColumn> columns,
      String relationshipName) {
    if (columns == null) {
      return;
    }
    for (RelationshipColumn column : columns) {
      if (!containsColumn(fkColumns, column.fkColumnId())) {
        throw new RelationshipColumnNotExistException(
            "FK column '%s' specified in relationship '%s' does not exist in the table".formatted(
                column.fkColumnId(),
                relationshipName));
      }
      if (!containsColumn(pkColumns, column.pkColumnId())) {
        throw new RelationshipColumnNotExistException(
            "PK column '%s' specified in relationship '%s' does not exist in the table".formatted(
                column.pkColumnId(),
                relationshipName));
      }
    }
  }

  public static void validateColumnUniqueness(
      List<RelationshipColumn> columns,
      String relationshipName) {
    if (columns == null) {
      return;
    }
    Set<String> fkColumnIds = new HashSet<>();
    Set<String> pkColumnIds = new HashSet<>();
    Set<String> pairs = new HashSet<>();
    for (RelationshipColumn column : columns) {
      String fkKey = normalizeId(column.fkColumnId());
      String pkKey = normalizeId(column.pkColumnId());
      if (!fkColumnIds.add(fkKey)) {
        throw new RelationshipColumnDuplicateException(
            "FK column '%s' is already mapped in relationship '%s'".formatted(
                column.fkColumnId(),
                relationshipName));
      }
      if (!pkColumnIds.add(pkKey)) {
        throw new RelationshipColumnDuplicateException(
            "PK column '%s' is already mapped in relationship '%s'".formatted(
                column.pkColumnId(),
                relationshipName));
      }
      String pairKey = fkKey + ":" + pkKey;
      if (!pairs.add(pairKey)) {
        throw new RelationshipColumnDuplicateException(
            "Relationship column mapping '%s:%s' is duplicated in relationship '%s'".formatted(
                column.fkColumnId(),
                column.pkColumnId(),
                relationshipName));
      }
    }
  }

  public static void validateIdentifyingCycle(
      List<Relationship> relationships,
      RelationshipKindChange pendingChange,
      Relationship newRelationship) {
    IdentifyingCycle cycle = detectIdentifyingCycle(relationships, pendingChange, newRelationship);
    if (cycle != null) {
      throw new RelationshipCyclicReferenceException(
          "Direct cyclic reference detected between tables: %s <-> %s".formatted(
              cycle.fromTableId(),
              cycle.toTableId()));
    }
  }

  public static IdentifyingCycle detectIdentifyingCycle(
      List<Relationship> relationships,
      RelationshipKindChange pendingChange,
      Relationship newRelationship) {
    if ((relationships == null || relationships.isEmpty()) && newRelationship == null) {
      return null;
    }
    Map<String, List<String>> graph = new HashMap<>();
    if (relationships != null) {
      for (Relationship relationship : relationships) {
        RelationshipKind effectiveKind = relationship.kind();
        if (pendingChange != null
            && equalsIgnoreCase(relationship.id(), pendingChange.relationshipId())) {
          effectiveKind = pendingChange.newKind();
        }
        if (effectiveKind != RelationshipKind.IDENTIFYING) {
          continue;
        }
        addEdge(graph, relationship.fkTableId(), relationship.pkTableId());
      }
    }
    if (newRelationship != null && newRelationship.kind() == RelationshipKind.IDENTIFYING) {
      addEdge(graph, newRelationship.fkTableId(), newRelationship.pkTableId());
    }
    return findCycle(graph);
  }

  private static IdentifyingCycle findCycle(Map<String, List<String>> graph) {
    Set<String> visiting = new HashSet<>();
    Set<String> visited = new HashSet<>();
    for (String start : graph.keySet()) {
      if (visited.contains(start)) {
        continue;
      }
      IdentifyingCycle cycle = dfs(start, graph, visiting, visited);
      if (cycle != null) {
        return cycle;
      }
    }
    return null;
  }

  private static IdentifyingCycle dfs(
      String current,
      Map<String, List<String>> graph,
      Set<String> visiting,
      Set<String> visited) {
    visiting.add(current);
    for (String next : graph.getOrDefault(current, List.of())) {
      if (visiting.contains(next)) {
        return new IdentifyingCycle(current, next);
      }
      if (!visited.contains(next)) {
        IdentifyingCycle cycle = dfs(next, graph, visiting, visited);
        if (cycle != null) {
          return cycle;
        }
      }
    }
    visiting.remove(current);
    visited.add(current);
    return null;
  }

  private static void addEdge(
      Map<String, List<String>> graph,
      String from,
      String to) {
    graph.computeIfAbsent(from, key -> new ArrayList<>()).add(to);
  }

  private static boolean containsColumn(List<Column> columns, String columnId) {
    if (columns == null) {
      return false;
    }
    for (Column column : columns) {
      if (equalsIgnoreCase(column.id(), columnId)) {
        return true;
      }
    }
    return false;
  }

  private static String normalizeId(String value) {
    return value == null ? null : value.toUpperCase(Locale.ROOT);
  }

  private static boolean equalsIgnoreCase(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.equalsIgnoreCase(right);
  }

  public record RelationshipKindChange(String relationshipId, RelationshipKind newKind) {
  }

  public record IdentifyingCycle(String fromTableId, String toTableId) {
  }

}
