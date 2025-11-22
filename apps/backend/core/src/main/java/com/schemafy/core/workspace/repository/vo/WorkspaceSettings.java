package com.schemafy.core.workspace.repository.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record WorkspaceSettings(
        @NotNull @Pattern(regexp = "^(light|dark)$") String theme,
        @NotNull @Pattern(regexp = "^(ko|en)$") String language,
        @JsonProperty("defaultProjectAccess") @Pattern(regexp = "^(viewer|editor)$") String defaultProjectAccess) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static WorkspaceSettings defaultSettings() {
        return new WorkspaceSettings("light", "ko", "viewer");
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
        if (theme == null || !theme.matches("^(light|dark)$")) {
            throw new IllegalArgumentException("Invalid theme: " + theme);
        }
        if (language == null || !language.matches("^(ko|en)$")) {
            throw new IllegalArgumentException("Invalid language: " + language);
        }
        if (defaultProjectAccess == null
                || !defaultProjectAccess.matches("^(viewer|editor)$")) {
            throw new IllegalArgumentException(
                    "Invalid defaultProjectAccess: " + defaultProjectAccess);
        }
    }
}
