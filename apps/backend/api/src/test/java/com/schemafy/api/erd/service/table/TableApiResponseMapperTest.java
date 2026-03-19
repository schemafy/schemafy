package com.schemafy.api.erd.service.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.table.domain.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TableApiResponseMapper")
class TableApiResponseMapperTest {

  private final TableApiResponseMapper sut = new TableApiResponseMapper(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @Test
  @DisplayName("table extra 문자열을 JSON 객체로 변환한다")
  void toTableResponse_mapsExtraJson() {
    Table table = new Table(
        "table-1",
        "schema-1",
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        "{\"position\":{\"x\":10,\"y\":20}}");

    var response = sut.toTableResponse(table);

    assertThat(response.extra()).isNotNull();
    assertThat(response.extra().get("position").get("x").intValue())
        .isEqualTo(10);
  }

  @Test
  @DisplayName("table extra가 null이면 그대로 null을 유지한다")
  void toTableResponse_preservesNullExtra() {
    Table table = new Table(
        "table-1",
        "schema-1",
        "users",
        "utf8mb4",
        "utf8mb4_general_ci");

    var response = sut.toTableResponse(table);

    assertThat(response.extra()).isNull();
  }

}
