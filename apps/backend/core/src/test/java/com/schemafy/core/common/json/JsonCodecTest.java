package com.schemafy.core.common.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JsonCodec")
class JsonCodecTest {

  private final JsonCodec sut = new JsonCodec(
      new ObjectMapper().findAndRegisterModules());

  @Test
  @DisplayName("persisted textual JSON을 object node로 복원한다")
  void parsePersistedNode_unwrapsTextualJson() {
    var node = sut.parsePersistedNode("\"{\\\"x\\\":1}\"");

    assertThat(node.isObject()).isTrue();
    assertThat(node.get("x").intValue()).isEqualTo(1);
  }

  @Test
  @DisplayName("serialize null은 IllegalArgumentException을 던진다")
  void serialize_nullThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.serialize(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be null");
  }

  @Test
  @DisplayName("serializeBytes null은 IllegalArgumentException을 던진다")
  void serializeBytes_nullThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.serializeBytes(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be null");
  }

  @Test
  @DisplayName("toObjectNode null은 IllegalArgumentException을 던진다")
  void toObjectNode_nullThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> sut.toObjectNode(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be null");
  }

}
