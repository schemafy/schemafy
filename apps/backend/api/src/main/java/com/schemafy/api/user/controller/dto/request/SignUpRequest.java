package com.schemafy.api.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.schemafy.core.user.application.port.in.SignUpUserCommand;
import com.schemafy.core.user.domain.UserPolicy;

public record SignUpRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,

    @NotBlank(message = "이름은 필수입니다.") @Size(max = UserPolicy.MAX_NAME_LENGTH, message = "이름은 200자를 초과할 수 없습니다.") String name,

    @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = UserPolicy.MIN_SIGN_UP_PASSWORD_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.") String password,

    @Size(min = 16, max = 128, message = "이메일 인증 토큰 형식이 올바르지 않습니다.") String signupVerificationToken) {

  public SignUpUserCommand toCommand() {
    return new SignUpUserCommand(email, name, password, signupVerificationToken);
  }

}
