package com.schemafy.core.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("이메일 값 객체")
class EmailTest {

  @Test
  @DisplayName("대문자 이메일을 소문자로 정규화한다")
  void createsNormalizedLowercaseEmail() {
    Email email = Email.from("USER@EXAMPLE.COM");

    assertThat(email.address()).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("잘못된 이메일 형식이면 예외를 던진다")
  void rejectsInvalidEmailFormat() {
    assertThatThrownBy(() -> Email.from("invalid-email"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid email format");
  }

}
