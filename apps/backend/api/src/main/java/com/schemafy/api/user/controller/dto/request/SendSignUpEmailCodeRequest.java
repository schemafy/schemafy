package com.schemafy.api.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.schemafy.core.user.application.port.in.SendSignUpEmailCodeCommand;

public record SendSignUpEmailCodeRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email) {

  public SendSignUpEmailCodeCommand toCommand() {
    return new SendSignUpEmailCodeCommand(email);
  }

}
