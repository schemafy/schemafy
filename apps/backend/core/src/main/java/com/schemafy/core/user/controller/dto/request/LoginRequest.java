package com.schemafy.core.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.schemafy.core.user.service.dto.LoginCommand;

public record LoginRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "유효한 이메일 형식이 아닙니다.") String email,

    @NotBlank(message = "비밀번호는 필수입니다.") String password) {

  public LoginCommand toCommand() {
    return new LoginCommand(email, password);
  }

}
