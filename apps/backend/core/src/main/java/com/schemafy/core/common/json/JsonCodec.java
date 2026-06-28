package com.schemafy.core.common.json;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Component
public final class JsonCodec {

  private final ObjectMapper objectMapper;

  public JsonCodec(ObjectMapper objectMapper) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper");
  }

  public String toJson(Object value) {
    requireNonNull(value, "value");
    try {
      return objectWriterFor(value).writeValueAsString(value);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to serialize JSON", e);
    }
  }

  public String toJson(Object value, Class<?> type) {
    requireNonNull(value, "value");
    requireNonNull(type, "type");
    try {
      return objectMapper.writerFor(type).writeValueAsString(value);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to serialize JSON", e);
    }
  }

  public <T> T fromJson(String rawJson, Class<T> type) {
    requireNonNull(rawJson, "rawJson");
    requireNonNull(type, "type");
    try {
      return objectMapper.readValue(rawJson, type);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
  }

  public <T> T fromPersistedJson(String rawJson, Class<T> type) {
    requireNonNull(type, "type");
    JsonNode node = parseOptionalPersistedNode(rawJson);
    if (node == null || node.isNull()) {
      return null;
    }
    if (JsonNode.class.isAssignableFrom(type)) {
      return type.cast(node);
    }
    return fromJson(toJson(node), type);
  }

  public String normalizePersistedJson(String rawJson) {
    JsonNode node = parseOptionalPersistedNode(rawJson);
    if (node == null || node.isNull()) {
      return null;
    }
    return toJson(node);
  }

  public byte[] toJsonBytes(Object value) {
    requireNonNull(value, "value");
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to serialize JSON", e);
    }
  }

  private JsonNode parseOptionalPersistedNode(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return null;
    }
    return parsePersistedNode(rawJson);
  }

  private JsonNode parsePersistedNode(String rawJson) {
    JsonNode node = readNode(rawJson);
    if (node.isTextual()) {
      return readNode(node.textValue());
    }
    return node;
  }

  private JsonNode readNode(String rawJson) {
    requireNonNull(rawJson, "rawJson");
    try {
      return objectMapper.readTree(rawJson);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
  }

  private ObjectWriter objectWriterFor(Object value) {
    Class<?> serializationType = polymorphicSerializationType(value.getClass());
    if (serializationType == null) {
      return objectMapper.writer();
    }
    return objectMapper.writerFor(serializationType);
  }

  private static Class<?> polymorphicSerializationType(Class<?> type) {
    if (type == null || type == Object.class) {
      return null;
    }
    if (type.isAnnotationPresent(JsonTypeInfo.class)) {
      return type;
    }
    Class<?> interfaceType = polymorphicInterfaceType(type);
    if (interfaceType != null) {
      return interfaceType;
    }
    return polymorphicSerializationType(type.getSuperclass());
  }

  private static Class<?> polymorphicInterfaceType(Class<?> type) {
    for (Class<?> interfaceType : type.getInterfaces()) {
      if (interfaceType.isAnnotationPresent(JsonTypeInfo.class)) {
        return interfaceType;
      }
      Class<?> nestedInterfaceType = polymorphicInterfaceType(interfaceType);
      if (nestedInterfaceType != null) {
        return nestedInterfaceType;
      }
    }
    return null;
  }

  private static <T> T requireNonNull(T value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return value;
  }

}
