package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @Size(min = 1, max = 255, message = "Project name must be between 1 and 255 characters") String name,
    String description) {

}
