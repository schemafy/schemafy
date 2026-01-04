package com.schemafy.core.project.repository.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record WorkspaceSettings(
    @NotNull @Pattern(regexp = "^(ko|en)$") String language) {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static WorkspaceSettings defaultSettings() {
    return new WorkspaceSettings("ko");
  }

  public String toJson() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Failed to serialize WorkspaceSettings", e);
    }
  }

  public static WorkspaceSettings fromJson(String json) {
    if (json == null || json.isBlank()) {
      return defaultSettings();
    }
    try {
      return objectMapper.readValue(json, WorkspaceSettings.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Failed to deserialize WorkspaceSettings: " + json, e);
    }
  }

  public void validate() {
    if (language == null || !language.matches("^(ko|en)$")) {
      throw new IllegalArgumentException("Invalid language: " + language);
    }
  }

}
