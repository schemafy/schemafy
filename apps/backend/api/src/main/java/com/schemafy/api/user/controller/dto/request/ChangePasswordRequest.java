package com.schemafy.api.user.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.schemafy.core.user.application.port.in.ChangePasswordCommand;
import com.schemafy.core.user.domain.UserPolicy;

public record ChangePasswordRequest(String currentPassword, String resetToken,
    @NotBlank @Size(min = UserPolicy.MIN_PASSWORD_LENGTH, max = UserPolicy.MAX_PASSWORD_LENGTH) String newPassword) {

  public ChangePasswordCommand toCommand() {
    return new ChangePasswordCommand(currentPassword, resetToken, newPassword);
  }

}
