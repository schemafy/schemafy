package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InversePayload JSON")
class InversePayloadJsonTest {

  private final JsonCodec jsonCodec = new JsonCodec(new ObjectMapper().findAndRegisterModules());

  @Test
  @DisplayName("column type arguments의 파생 empty 프로퍼티를 직렬화하지 않는다")
  void serializeChangeColumnTypeInverse_excludesDerivedEmptyProperty() {
    ChangeColumnTypeInverse payload = new ChangeColumnTypeInverse(
        "column-1",
        "VARCHAR",
        new ColumnTypeArguments(255, null, null),
        List.of(new ChangeColumnTypeInverse.FkColumnTypeRevert(
            "fk-column-1",
            "VARCHAR",
            new ColumnTypeArguments(255, null, null),
            "utf8mb4",
            "utf8mb4_general_ci")));

    String json = jsonCodec.toJson(payload);
    InversePayload parsed = jsonCodec.fromJson(json, InversePayload.class);

    assertThat(json).doesNotContain("\"empty\"");
    assertThat(parsed).isEqualTo(payload);
  }

  @Test
  @DisplayName("structural snapshot의 column type arguments도 empty 없이 복원한다")
  void serializeStructuralInverse_excludesNestedDerivedEmptyProperty() {
    StructuralSnapshot snapshot = new StructuralSnapshot(
        "schema-1",
        List.of(),
        List.of(new StructuralSnapshot.ColumnSnapshot(
            "column-1",
            "table-1",
            "name",
            "VARCHAR",
            new ColumnTypeArguments(255, null, null),
            0,
            false,
            null,
            null,
            null)),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
    AddIndexColumnInverse payload = new AddIndexColumnInverse(
        "schema-1",
        "index-column-1",
        snapshot,
        snapshot,
        List.of("table-1"));

    String json = jsonCodec.toJson(payload);
    InversePayload parsed = jsonCodec.fromJson(json, InversePayload.class);

    assertThat(json).doesNotContain("\"empty\"");
    assertThat(parsed).isEqualTo(payload);
  }

  @Test
  @DisplayName("create/delete structural inverse는 table snapshot을 포함해 복원한다")
  void serializeCreateTableInverse_includesTableSnapshot() {
    StructuralSnapshot beforeSnapshot = new StructuralSnapshot(
        "schema-1",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
    StructuralSnapshot afterSnapshot = new StructuralSnapshot(
        "schema-1",
        List.of(new StructuralSnapshot.TableSnapshot(
            "table-1",
            "schema-1",
            "users",
            "utf8mb4",
            "utf8mb4_general_ci",
            "{\"x\":100}")),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
    CreateTableInverse payload = new CreateTableInverse(
        "schema-1",
        "table-1",
        beforeSnapshot,
        afterSnapshot,
        List.of("table-1"));

    String json = jsonCodec.toJson(payload);
    InversePayload parsed = jsonCodec.fromJson(json, InversePayload.class);

    assertThat(json).contains("\"kind\":\"CREATE_TABLE\"");
    assertThat(json).contains("\"tables\"");
    assertThat(parsed).isEqualTo(payload);
  }

  @Test
  @DisplayName("기존에 empty가 포함된 inverse JSON도 복원한다")
  void parseInversePayload_ignoresLegacyDerivedEmptyProperty() {
    String json = """
        {
          "kind": "CHANGE_COLUMN_TYPE",
          "columnId": "column-1",
          "oldDataType": "VARCHAR",
          "oldTypeArguments": {
            "length": 255,
            "precision": null,
            "scale": null,
            "values": null,
            "empty": false
          },
          "fkRevertList": []
        }
        """;

    InversePayload parsed = jsonCodec.fromJson(json, InversePayload.class);

    assertThat(parsed).isEqualTo(new ChangeColumnTypeInverse(
        "column-1",
        "VARCHAR",
        new ColumnTypeArguments(255, null, null),
        List.of()));
  }

}
