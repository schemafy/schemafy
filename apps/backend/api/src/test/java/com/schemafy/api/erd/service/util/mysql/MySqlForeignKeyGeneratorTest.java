package com.schemafy.api.erd.service.util.mysql;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

import static org.junit.jupiter.api.Assertions.*;

class MySqlForeignKeyGeneratorTest {

  private MySqlForeignKeyGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new MySqlForeignKeyGenerator();
  }

  @Test
  void generate_withForeignKey_returnsAlterStatement() {
    RelationshipColumnResponse column = new RelationshipColumnResponse(
        "rc1", "fk1", "col2", "col1", 1);

    RelationshipSnapshotResponse fk = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk1", "t1", "t2", "fk_user_id",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "orders", null, null, null),
        null, null, List.of(fk), null);

    Map<String, String> tableIdToName = Map.of("t2", "users");
    Map<String, String> columnIdToName = Map.of("col1", "user_id");
    Map<String, String> pkColumnIdToName = Map.of("col2", "id");
    Map<String, Map<String, String>> tableColumnMaps = Map.of("t2", pkColumnIdToName);

    List<String> result = generator.generate(table, tableIdToName, columnIdToName, tableColumnMaps);

    assertEquals(1, result.size());
    assertEquals(
        "ALTER TABLE `orders` ADD CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);",
        result.get(0));
  }

  @Test
  void generate_withCompositeKey_ordersColumnsCorrectly() {
    RelationshipColumnResponse col1 = new RelationshipColumnResponse(
        "rc1", "fk1", "pk_col1", "fk_col1", 2);

    RelationshipColumnResponse col2 = new RelationshipColumnResponse(
        "rc2", "fk1", "pk_col2", "fk_col2", 1);

    RelationshipSnapshotResponse fk = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk1", "t1", "t2", "fk_composite",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        List.of(col1, col2));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "order_items", null, null, null),
        null, null, List.of(fk), null);

    Map<String, String> tableIdToName = Map.of("t2", "orders");
    Map<String, String> columnIdToName = Map.of("fk_col1", "item_id", "fk_col2", "order_id");
    Map<String, String> pkColumnIdToName = Map.of("pk_col1", "id", "pk_col2", "order_id");
    Map<String, Map<String, String>> tableColumnMaps = Map.of("t2", pkColumnIdToName);

    List<String> result = generator.generate(table, tableIdToName, columnIdToName, tableColumnMaps);

    assertEquals(1, result.size());
    assertTrue(result.get(0).contains("(`order_id`, `item_id`)"));
    assertTrue(result.get(0).contains("REFERENCES `orders` (`order_id`, `id`)"));
  }

  @Test
  void generate_filtersOnlyCurrentTableForeignKeys() {
    RelationshipColumnResponse col1 = new RelationshipColumnResponse(
        "rc1", "fk1", "col2", "col1", 1);

    RelationshipSnapshotResponse fkOwned = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk1", "t1", "t2", "fk_owned",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        List.of(col1));

    RelationshipSnapshotResponse fkNotOwned = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk2", "t3", "t1", "fk_not_owned",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        List.of(col1));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "orders", null, null, null),
        null, null, List.of(fkOwned, fkNotOwned), null);

    Map<String, String> tableIdToName = Map.of("t2", "users", "t3", "invoices");
    Map<String, String> columnIdToName = Map.of("col1", "user_id");
    Map<String, String> pkColumnIdToName = Map.of("col2", "id");
    Map<String, Map<String, String>> tableColumnMaps = Map.of("t2", pkColumnIdToName);

    List<String> result = generator.generate(table, tableIdToName, columnIdToName, tableColumnMaps);

    assertEquals(1, result.size());
    assertTrue(result.get(0).contains("fk_owned"));
    assertFalse(result.get(0).contains("fk_not_owned"));
  }

  @Test
  void generate_withNoRelationships_returnsEmptyList() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, null, Collections.emptyList(), null);

    List<String> result = generator.generate(table, Collections.emptyMap(),
        Collections.emptyMap(), Collections.emptyMap());

    assertTrue(result.isEmpty());
  }

  @Test
  void generate_withNullRelationships_returnsEmptyList() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, null, null, null);

    List<String> result = generator.generate(table, Collections.emptyMap(),
        Collections.emptyMap(), Collections.emptyMap());

    assertTrue(result.isEmpty());
  }

  @Test
  void generate_withBlankTableName_throwsException() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "", null, null, null),
        null, null, null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap()));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
  }

  @Test
  void generate_withNullTableName_throwsException() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, null, null, null, null),
        null, null, null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap()));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
  }

  @Test
  void generate_withBlankForeignKeyName_throwsException() {
    RelationshipColumnResponse column = new RelationshipColumnResponse(
        "rc1", "fk1", "col2", "col1", 1);

    RelationshipSnapshotResponse fk = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk1", "t1", "t2", "",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "orders", null, null, null),
        null, null, List.of(fk), null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Map.of("t2", "users"),
            Map.of("col1", "user_id"), Map.of("t2", Map.of("col2", "id"))));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
  }

  @Test
  void generate_withReferencedTableNotFound_throwsException() {
    RelationshipColumnResponse column = new RelationshipColumnResponse(
        "rc1", "fk1", "col2", "col1", 1);

    RelationshipSnapshotResponse fk = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk1", "t1", "t2", "fk_user_id",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "orders", null, null, null),
        null, null, List.of(fk), null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Collections.emptyMap(),
            Map.of("col1", "user_id"), Collections.emptyMap()));
    assertEquals(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void generate_withEmptyFkColumns_throwsException() {
    RelationshipSnapshotResponse fk = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk1", "t1", "t2", "fk_empty",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        Collections.emptyList());

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "orders", null, null, null),
        null, null, List.of(fk), null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Map.of("t2", "users"),
            Collections.emptyMap(), Collections.emptyMap()));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
  }

  @Test
  void generate_withSpecialCharactersInNames_escapesCorrectly() {
    RelationshipColumnResponse column = new RelationshipColumnResponse(
        "rc1", "fk1", "col2", "col1", 1);

    RelationshipSnapshotResponse fk = new RelationshipSnapshotResponse(
        new RelationshipResponse("fk1", "t1", "t2", "fk`special",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "order`table", null, null, null),
        null, null, List.of(fk), null);

    Map<String, String> tableIdToName = Map.of("t2", "user`table");
    Map<String, String> columnIdToName = Map.of("col1", "user`id");
    Map<String, String> pkColumnIdToName = Map.of("col2", "id");
    Map<String, Map<String, String>> tableColumnMaps = Map.of("t2", pkColumnIdToName);

    List<String> result = generator.generate(table, tableIdToName, columnIdToName, tableColumnMaps);

    assertEquals(1, result.size());
    assertTrue(result.get(0).contains("`order``table`"));
    assertTrue(result.get(0).contains("`fk``special`"));
    assertTrue(result.get(0).contains("`user``id`"));
    assertTrue(result.get(0).contains("`user``table`"));
  }

}
