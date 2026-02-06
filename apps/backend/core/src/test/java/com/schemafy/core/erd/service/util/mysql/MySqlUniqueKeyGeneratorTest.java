package com.schemafy.core.erd.service.util.mysql;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.CommonErrorCode;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

import static org.junit.jupiter.api.Assertions.*;

class MySqlUniqueKeyGeneratorTest {

  private MySqlUniqueKeyGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new MySqlUniqueKeyGenerator();
  }

  @Test
  void generate_withUniqueConstraint_returnsAlterStatement() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "uk1", "col1", 1);

    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    Map<String, String> columnIdToName = Map.of("col1", "email");

    List<String> result = generator.generate(table, columnIdToName);

    assertEquals(1, result.size());
    assertEquals("ALTER TABLE `users` ADD UNIQUE KEY `uk_email` (`email`);", result.get(0));
  }

  @Test
  void generate_withMultipleUniqueConstraints_returnsMultipleStatements() {
    ConstraintColumnResponse col1 = new ConstraintColumnResponse("cc1", "uk1", "col1", 1);
    ConstraintColumnResponse col2 = new ConstraintColumnResponse("cc2", "uk2", "col2", 1);

    ConstraintSnapshotResponse uk1 = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
        List.of(col1));

    ConstraintSnapshotResponse uk2 = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk2", "t1", "uk_username", ConstraintKind.UNIQUE, null, null),
        List.of(col2));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(uk1, uk2), null, null);

    Map<String, String> columnIdToName = Map.of("col1", "email", "col2", "username");

    List<String> result = generator.generate(table, columnIdToName);

    assertEquals(2, result.size());
  }

  @Test
  void generate_withCompositeUniqueKey_ordersColumnsCorrectly() {
    ConstraintColumnResponse col1 = new ConstraintColumnResponse("cc1", "uk1", "col1", 2);
    ConstraintColumnResponse col2 = new ConstraintColumnResponse("cc2", "uk1", "col2", 1);

    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_tenant_email", ConstraintKind.UNIQUE, null, null),
        List.of(col1, col2));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    Map<String, String> columnIdToName = Map.of("col1", "email", "col2", "tenant_id");

    List<String> result = generator.generate(table, columnIdToName);

    assertEquals(1, result.size());
    assertEquals("ALTER TABLE `users` ADD UNIQUE KEY `uk_tenant_email` (`tenant_id`, `email`);",
        result.get(0));
  }

  @Test
  void generate_withNoUniqueConstraints_returnsEmptyList() {
    ConstraintSnapshotResponse pk = new ConstraintSnapshotResponse(
        new ConstraintResponse("pk1", "t1", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
        Collections.emptyList());

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(pk), null, null);

    List<String> result = generator.generate(table, Collections.emptyMap());

    assertTrue(result.isEmpty());
  }

  @Test
  void generate_withNullConstraints_returnsEmptyList() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, null, null, null);

    List<String> result = generator.generate(table, Collections.emptyMap());

    assertTrue(result.isEmpty());
  }

  @Test
  void generate_withNullTableName_throwsException() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, null, null, null, null),
        null, null, null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Collections.emptyMap()));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE,
        exception.getErrorCode());
  }

  @Test
  void generate_withBlankTableName_throwsException() {
    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "", null, null, null),
        null, null, null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Collections.emptyMap()));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE,
        exception.getErrorCode());
  }

  @Test
  void generate_withNullConstraintName_throwsException() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "uk1", "col1", 1);

    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", null, ConstraintKind.UNIQUE, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Map.of("col1", "email")));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE,
        exception.getErrorCode());
  }

  @Test
  void generate_withBlankConstraintName_throwsException() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "uk1", "col1", 1);

    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "", ConstraintKind.UNIQUE, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Map.of("col1", "email")));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE,
        exception.getErrorCode());
  }

  @Test
  void generate_withNullColumns_throwsException() {
    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
        null);

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Collections.emptyMap()));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE,
        exception.getErrorCode());
  }

  @Test
  void generate_withEmptyColumns_throwsException() {
    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
        Collections.emptyList());

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Collections.emptyMap()));
    assertEquals(CommonErrorCode.INVALID_INPUT_VALUE,
        exception.getErrorCode());
  }

  @Test
  void generate_withUnknownColumnId_throwsException() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "uk1", "unknown_col", 1);

    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "users", null, null, null),
        null, List.of(unique), null, null);

    DomainException exception = assertThrows(DomainException.class,
        () -> generator.generate(table, Map.of("col1", "email")));
    assertEquals(ColumnErrorCode.NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void generate_withSpecialCharactersInTableName_escapesCorrectly() {
    ConstraintColumnResponse column = new ConstraintColumnResponse("cc1", "uk1", "col1", 1);

    ConstraintSnapshotResponse unique = new ConstraintSnapshotResponse(
        new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
        List.of(column));

    TableSnapshotResponse table = new TableSnapshotResponse(
        new TableResponse("t1", null, "user`table", null, null, null),
        null, List.of(unique), null, null);

    Map<String, String> columnIdToName = Map.of("col1", "email");

    List<String> result = generator.generate(table, columnIdToName);

    assertEquals(1, result.size());
    assertEquals("ALTER TABLE `user``table` ADD UNIQUE KEY `uk_email` (`email`);", result.get(0));
  }

}
