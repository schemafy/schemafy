package com.schemafy.api.project.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateProjectShareLinkRequest(@NotNull Boolean isActive) {
}
