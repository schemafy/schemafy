package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required") @Size(min = 1, max = 255, message = "Workspace name must be between 1 and 255 characters") String name,
        @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
        WorkspaceSettings settings) {

    @JsonIgnore
    public WorkspaceSettings getSettingsOrDefault() {
        return settings != null ? settings
                : WorkspaceSettings.defaultSettings();
    }

}
