package com.schemafy.core.erd.service.util.mysql;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MySqlPrimaryKeyGeneratorTest {

  private MySqlPrimaryKeyGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new MySqlPrimaryKeyGenerator();
  }

  @Test
  void generate_withPrimaryKey_returnsAlterStatement() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "pk1", "col1", 1);

    ConstraintSnapshotResponse pk = new ConstraintSnapshotResponse(
        new ConstraintResponse("pk1", "t1", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(pk), null, null);

    Map<String, String> columnIdToName = Map.of("col1", "id");

    Optional<String> result = generator.generate(table, columnIdToName);

    assertTrue(result.isPresent());
    assertEquals("ALTER TABLE `users` ADD PRIMARY KEY (`id`);", result.get());
  }

  @Test
  void generate_withMultipleColumns_ordersCorrectly() {
    ConstraintColumnResponse col1 = new ConstraintColumnResponse("cc1", "pk1", "col1", 2);
    ConstraintColumnResponse col2 = new ConstraintColumnResponse("cc2", "pk1", "col2", 1);

    ConstraintSnapshotResponse pk = new ConstraintSnapshotResponse(
        new ConstraintResponse("pk1", "t1", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
        List.of(col1, col2));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "orders", null, null, null),
        null, List.of(pk), null, null);

    Map<String, String> columnIdToName = Map.of("col1", "order_id", "col2", "user_id");

    Optional<String> result = generator.generate(table, columnIdToName);

    assertTrue(result.isPresent());
    assertEquals("ALTER TABLE `orders` ADD PRIMARY KEY (`user_id`, `order_id`);", result.get());
  }

  @Test
  void generate_withNoPrimaryKey_returnsEmpty() {
    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
        Collections.emptyList());

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    Optional<String> result = generator.generate(table, Collections.emptyMap());

    assertTrue(result.isEmpty());
  }

  @Test
  void generate_withNullConstraints_returnsEmpty() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, null, null, null);

    Optional<String> result = generator.generate(table, Collections.emptyMap());

    assertTrue(result.isEmpty());
  }

  @Test
  void generate_withBlankTableName_throwsException() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "", null, null, null),
        null, null, null, null);

    assertThrows(BusinessException.class,
        () -> generator.generate(table, Collections.emptyMap()));
  }

  @Test
  void generate_withNullTableName_throwsException() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, null, null, null, null),
        null, null, null, null);

    assertThrows(BusinessException.class,
        () -> generator.generate(table, Collections.emptyMap()));
  }

  @Test
  void generate_withEmptyColumns_throwsException() {
    ConstraintSnapshotResponse pk = new ConstraintSnapshotResponse(
        new ConstraintResponse("pk1", "t1", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
        Collections.emptyList());

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(pk), null, null);

    assertThrows(IllegalArgumentException.class,
        () -> generator.generate(table, Collections.emptyMap()));
  }

  @Test
  void generate_withUnknownColumnId_throwsException() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "pk1", "unknown_col", 1);

    ConstraintSnapshotResponse pk = new ConstraintSnapshotResponse(
        new ConstraintResponse("pk1", "t1", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(pk), null, null);

    assertThrows(BusinessException.class,
        () -> generator.generate(table, Map.of("col1", "id")));
  }

  @Test
  void generate_withSpecialCharactersInTableName_escapesCorrectly() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "pk1", "col1", 1);

    ConstraintSnapshotResponse pk = new ConstraintSnapshotResponse(
        new ConstraintResponse("pk1", "t1", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "user`table", null, null, null),
        null, List.of(pk), null, null);

    Map<String, String> columnIdToName = Map.of("col1", "id");

    Optional<String> result = generator.generate(table, columnIdToName);

    assertTrue(result.isPresent());
    assertEquals("ALTER TABLE `user``table` ADD PRIMARY KEY (`id`);", result.get());
  }

}
