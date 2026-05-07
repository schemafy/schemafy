package com.schemafy.core.common.json;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public final class JsonCodec {

  private final ObjectMapper objectMapper;

  public JsonCodec(ObjectMapper objectMapper) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper");
  }

  public String canonicalize(String rawJson) {
    return canonicalize(parseNode(rawJson));
  }

  public String canonicalize(JsonNode node) {
    requireNonNull(node, "node");
    try {
      return objectMapper.writeValueAsString(node);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to canonicalize JSON", e);
    }
  }

  public String canonicalizeOptional(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return null;
    }
    return canonicalize(rawJson);
  }

  public String canonicalizeOptional(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return canonicalize(node);
  }

  public <T> T parse(String rawJson, Class<T> type) {
    requireNonNull(rawJson, "rawJson");
    requireNonNull(type, "type");
    try {
      return objectMapper.readValue(rawJson, type);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
  }

  public JsonNode parseNode(String rawJson) {
    requireNonNull(rawJson, "rawJson");
    try {
      return objectMapper.readTree(rawJson);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
  }

  public JsonNode parseOptionalNode(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return null;
    }
    return parseNode(rawJson);
  }

  public String normalizePersistedJson(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return null;
    }

    JsonNode node = parsePersistedNode(rawJson);
    if (node.isNull()) {
      return null;
    }
    return canonicalize(node);
  }

  public String serialize(Object value) {
    requireNonNull(value, "value");
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to serialize JSON", e);
    }
  }

  public byte[] serializeBytes(Object value) {
    requireNonNull(value, "value");
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to serialize JSON", e);
    }
  }

  public ObjectNode toObjectNode(Object value) {
    requireNonNull(value, "value");
    return objectMapper.valueToTree(value);
  }

  public JsonNode parsePersistedNode(String rawJson) {
    JsonNode node = parseNode(rawJson);
    if (node.isTextual()) {
      return parseNode(node.textValue());
    }
    return node;
  }

  private static <T> T requireNonNull(T value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return value;
  }

}
