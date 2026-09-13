package com.schemafy.core.common.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JsonObjectMetadataConverter")
class JsonObjectMetadataConverterTest {

  private final JsonObjectMetadataConverter sut = new JsonObjectMetadataConverter(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @Test
  @DisplayName("toStorageJson은 JSON object를 canonical storage string으로 변환한다")
  void toStorageJson_returnsCanonicalStorageJson() {
    JsonNode node = sut.toJsonNode("{\"ui\": {\"x\": 100, \"y\": 200}}");

    assertThat(sut.toStorageJson(node))
        .isEqualTo("{\"ui\":{\"x\":100,\"y\":200}}");
  }

  @Test
  @DisplayName("toStorageJson은 null과 JSON null을 null로 유지한다")
  void toStorageJson_preservesNull() {
    assertThat(sut.toStorageJson(null)).isNull();
    assertThat(sut.toStorageJson(sut.toJsonNode("null"))).isNull();
  }

  @Test
  @DisplayName("toStorageJson은 JSON object가 아니면 예외를 던진다")
  void toStorageJson_rejectsNonObject() {
    assertThatThrownBy(() -> sut.toStorageJson(TextNode.valueOf("invalid")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("JSON object metadata is required");
  }

  @Test
  @DisplayName("toOptionalJsonNode는 null과 blank를 null로 유지한다")
  void toOptionalJsonNode_preservesEmptyValue() {
    assertThat(sut.toOptionalJsonNode(null)).isNull();
    assertThat(sut.toOptionalJsonNode(" ")).isNull();
  }

}
