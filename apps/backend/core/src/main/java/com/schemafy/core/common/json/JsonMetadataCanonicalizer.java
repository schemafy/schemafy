package com.schemafy.core.common.json;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class JsonMetadataCanonicalizer {

  private final JsonCodec jsonCodec;

  public JsonMetadataCanonicalizer(JsonCodec jsonCodec) {
    this.jsonCodec = jsonCodec;
  }

  public String toOptionalJsonObject(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isObject()) {
      throw new IllegalArgumentException("JSON object metadata is required");
    }
    return jsonCodec.toJson(node);
  }

}
