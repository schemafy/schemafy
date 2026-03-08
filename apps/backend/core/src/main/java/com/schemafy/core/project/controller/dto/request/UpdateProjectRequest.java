package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectRequest(
    @NotBlank(message = "Project name is required") String name,
    String description) {

}
