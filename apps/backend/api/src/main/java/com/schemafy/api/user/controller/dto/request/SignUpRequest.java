package com.schemafy.api.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.schemafy.core.user.application.port.in.SignUpUserCommand;

public record SignUpRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,

    @NotBlank(message = "이름은 필수입니다.") String name,

    @NotBlank(message = "비밀번호는 필수입니다.") String password) {

  public SignUpUserCommand toCommand() {
    return new SignUpUserCommand(email, name, password);
  }

}
