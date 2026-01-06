package com.schemafy.core.project.controller.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateShareLinkRequest(

    @NotBlank(message = "Role is required") @Pattern(regexp = "viewer|commenter|editor", message = "Role must be viewer, commenter, or editor") String role,

    Instant expiresAt) {
}
