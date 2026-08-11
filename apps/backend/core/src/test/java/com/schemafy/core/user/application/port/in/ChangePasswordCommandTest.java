package com.schemafy.core.user.application.port.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChangePasswordCommand")
class ChangePasswordCommandTest {

  @Test
  @DisplayName("새 비밀번호가 정책을 만족하지 않으면 INVALID_PARAMETER를 반환한다")
  void rejectsInvalidNewPassword() {
    assertThatThrownBy(() -> new ChangePasswordCommand(
        "current-password", null, "passwrd"))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("새 비밀번호가 BCrypt UTF-8 72바이트 한도를 넘으면 INVALID_PARAMETER를 반환한다")
  void rejectsNewPasswordExceedingBcryptByteLimit() {
    assertThatThrownBy(() -> new ChangePasswordCommand(
        "current-password", null, "가".repeat(25)))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

}
