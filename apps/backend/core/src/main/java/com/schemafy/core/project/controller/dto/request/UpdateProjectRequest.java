package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.schemafy.core.project.repository.vo.ProjectSettings;

public record UpdateProjectRequest(
        @NotBlank(message = "Project name is required") String name,
        String description, ProjectSettings settings) {

    @JsonIgnore
    public ProjectSettings getSettingsOrDefault() {
        return settings != null ? settings : ProjectSettings.defaultSettings();
    }

}
