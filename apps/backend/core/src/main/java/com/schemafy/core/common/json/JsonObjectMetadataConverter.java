package com.schemafy.core.common.json;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class JsonObjectMetadataConverter {

  private final JsonCodec jsonCodec;

  public JsonObjectMetadataConverter(JsonCodec jsonCodec) {
    this.jsonCodec = jsonCodec;
  }

  public String toStorageJson(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isObject()) {
      throw new IllegalArgumentException("JSON object metadata is required");
    }
    return jsonCodec.toJson(node);
  }

  public JsonNode toJsonNode(String storageJson) {
    return jsonCodec.fromJson(storageJson, JsonNode.class);
  }

  public JsonNode toOptionalJsonNode(String storageJson) {
    if (storageJson == null || storageJson.isBlank()) {
      return null;
    }
    return toJsonNode(storageJson);
  }

}
