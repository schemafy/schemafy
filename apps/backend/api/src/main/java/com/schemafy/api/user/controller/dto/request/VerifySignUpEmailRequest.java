package com.schemafy.api.user.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.schemafy.core.user.application.port.in.VerifySignUpEmailCommand;

public record VerifySignUpEmailRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,

    @NotBlank(message = "인증 코드는 필수입니다.") @Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다.") String code) {

  public VerifySignUpEmailCommand toCommand() {
    return new VerifySignUpEmailCommand(email, code);
  }

}
