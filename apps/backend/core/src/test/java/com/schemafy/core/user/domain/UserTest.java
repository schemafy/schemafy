package com.schemafy.core.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 도메인")
class UserTest {

  @Test
  @DisplayName("회원가입 유저 생성 시 이름 정책을 만족해야 한다")
  void signUp_validatesNamePolicy() {
    assertThatThrownBy(() -> User.signUp(
        "user-1",
        "test@example.com",
        "a".repeat(201),
        "encoded"))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("회원가입 유저 생성 시 encoded password가 있어야 한다")
  void signUp_validatesEncodedPassword() {
    assertThatThrownBy(() -> User.signUp(
        "user-1",
        "test@example.com",
        "Tester",
        " "))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("OAuth 회원가입 유저 생성 시에도 이름 정책을 만족해야 한다")
  void signUpOAuth_validatesNamePolicy() {
    assertThatThrownBy(() -> User.signUpOAuth(
        "user-1",
        "test@example.com",
        " "))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("회원가입 유저 생성 시 정책을 만족하면 통과한다")
  void signUp_success() {
    assertThatCode(() -> User.signUp(
        "user-1",
        "test@example.com",
        "Tester",
        "encoded"))
        .doesNotThrowAnyException();
  }

}
