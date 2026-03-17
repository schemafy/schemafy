package com.schemafy.core.common.json;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonCodec {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .findAndRegisterModules();

  private JsonCodec() {
  }

  public static String canonicalize(String rawJson) {
    return canonicalize(parseNode(rawJson));
  }

  public static String canonicalize(JsonNode node) {
    Objects.requireNonNull(node, "node must not be null");
    try {
      return OBJECT_MAPPER.writeValueAsString(node);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to canonicalize JSON", e);
    }
  }

  public static String canonicalizeOptional(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return null;
    }
    return canonicalize(rawJson);
  }

  public static String canonicalizeOptional(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return canonicalize(node);
  }

  public static <T> T parse(String rawJson, Class<T> type) {
    Objects.requireNonNull(type, "type must not be null");
    try {
      return OBJECT_MAPPER.readValue(rawJson, type);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
  }

  public static JsonNode parseNode(String rawJson) {
    try {
      return OBJECT_MAPPER.readTree(rawJson);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
  }

  public static JsonNode parseOptionalNode(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return null;
    }
    return parseNode(rawJson);
  }

  public static String normalizePersistedJson(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return rawJson;
    }

    JsonNode node = parseNode(rawJson);
    if (node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return canonicalize(node.textValue());
    }
    return canonicalize(node);
  }

}
