package com.schemafy.core.common.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JsonCodec")
class JsonCodecTest {

  private final JsonCodec sut = new JsonCodec(
      new ObjectMapper().findAndRegisterModules());

  @Test
  @DisplayName("toJson은 JSON을 compact canonical string으로 직렬화한다")
  void toJson_returnsCompactJson() {
    JsonNode node = sut.fromJson("{\"b\":2, \"a\":1}", JsonNode.class);

    assertThat(sut.toJson(node)).isEqualTo("{\"b\":2,\"a\":1}");
  }

  @Test
  @DisplayName("toJsonNode는 객체의 JSON 구조를 보존한다")
  void toJsonNode_preservesObjectStructure() {
    JsonNode node = sut.toJsonNode(new TestPayload(1));

    assertThat(node.path("x").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("toJsonNode null은 IllegalArgumentException을 던진다")
  void toJsonNode_nullThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.toJsonNode(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be null");
  }

  @Test
  @DisplayName("fromJson은 raw JSON을 지정 타입으로 역직렬화한다")
  void fromJson_parsesRawJson() {
    TestPayload payload = sut.fromJson("{\"x\":1}", TestPayload.class);

    assertThat(payload).isEqualTo(new TestPayload(1));
  }

  @Test
  @DisplayName("normalizePersistedJson blank는 null로 정규화한다")
  void normalizePersistedJson_blankReturnsNull() {
    assertThat(sut.normalizePersistedJson("  ")).isNull();
  }

  @Test
  @DisplayName("normalizePersistedJson은 persisted textual JSON을 canonical JSON으로 정규화한다")
  void normalizePersistedJson_unwrapsTextualJson() {
    assertThat(sut.normalizePersistedJson("\"{\\\"x\\\":1}\""))
        .isEqualTo("{\"x\":1}");
  }

  @Test
  @DisplayName("normalizePersistedJson JSON null은 null로 정규화한다")
  void normalizePersistedJson_jsonNullReturnsNull() {
    assertThat(sut.normalizePersistedJson("null")).isNull();
  }

  @Test
  @DisplayName("fromPersistedJson은 persisted textual JSON을 지정 타입으로 복원한다")
  void fromPersistedJson_unwrapsTextualJson() {
    JsonNode node = sut.fromPersistedJson("\"{\\\"x\\\":1}\"",
        JsonNode.class);

    assertThat(node.isObject()).isTrue();
    assertThat(node.get("x").intValue()).isEqualTo(1);
  }

  @Test
  @DisplayName("fromPersistedJson blank는 null로 복원한다")
  void fromPersistedJson_blankReturnsNull() {
    assertThat(sut.fromPersistedJson("  ", TestPayload.class)).isNull();
  }

  @Test
  @DisplayName("toJson null은 IllegalArgumentException을 던진다")
  void toJson_nullThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.toJson(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be null");
  }

  @Test
  @DisplayName("toJson type null은 IllegalArgumentException을 던진다")
  void toJson_nullTypeThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.toJson(new TestPayload(1), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("type must not be null");
  }

  @Test
  @DisplayName("fromJson null은 IllegalArgumentException을 던진다")
  void fromJson_nullThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.fromJson(null, TestPayload.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rawJson must not be null");
  }

  @Test
  @DisplayName("toJsonBytes null은 IllegalArgumentException을 던진다")
  void toJsonBytes_nullThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.toJsonBytes(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be null");
  }

  @Test
  @DisplayName("fromPersistedJson type null은 IllegalArgumentException을 던진다")
  void fromPersistedJson_nullTypeThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.fromPersistedJson("{}", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("type must not be null");
  }

  private record TestPayload(int x) {
  }

}
