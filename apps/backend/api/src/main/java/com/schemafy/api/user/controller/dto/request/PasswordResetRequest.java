package com.schemafy.api.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.schemafy.core.user.application.port.in.RequestPasswordResetCommand;

public record PasswordResetRequest(
    @NotBlank @Email String email) {

  public RequestPasswordResetCommand toCommand() {
    return new RequestPasswordResetCommand(email);
  }

}
