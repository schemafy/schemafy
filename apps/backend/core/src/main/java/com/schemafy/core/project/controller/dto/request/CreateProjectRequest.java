package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.schemafy.core.project.repository.vo.ProjectSettings;

public record CreateProjectRequest(
        @Size(min = 1, max = 255, message = "Project name must be between 1 and 255 characters") String name,
        String description, ProjectSettings settings) {

    @JsonIgnore
    public ProjectSettings getSettingsOrDefault() {
        return settings != null ? settings : ProjectSettings.defaultSettings();
    }

}
