package com.schemafy.core.project.repository.vo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;

public record ProjectSettings(String theme, String language) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ProjectSettings defaultSettings() {
        return new ProjectSettings("light", "en");
    }

    public void validate() {
        if (theme == null || theme.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (language == null || language.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public static ProjectSettings fromJson(String json) {
        if (json == null || json.isBlank()) {
            return defaultSettings();
        }
        try {
            return objectMapper.readValue(json, ProjectSettings.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

}
