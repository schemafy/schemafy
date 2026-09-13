package com.schemafy.core.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.ChangePasswordCommand;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.application.port.out.PasswordHashPort;
import com.schemafy.core.user.application.port.out.UpdateUserPasswordPort;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("비밀번호 변경 서비스")
class ChangePasswordServiceTest {

  @Mock
  FindUserByIdPort findUserByIdPort;
  @Mock
  PasswordHashPort passwordHashPort;
  @Mock
  UpdateUserPasswordPort updateUserPasswordPort;
  @Mock
  AuthTokenPort authTokenPort;
  @InjectMocks
  ChangePasswordService sut;

  @Test
  @DisplayName("유효한 재설정 token은 한 번 소비하고 새 비밀번호를 저장한다")
  void changePassword_validResetTokenConsumesAndUpdatesPassword() {
    given(authTokenPort.consume(AuthTokenType.PASSWORD_RESET, "user-id", "raw-token"))
        .willReturn(Mono.just(AuthTokenConsumeResult.CONSUMED));
    given(passwordHashPort.hash("new-password")).willReturn(Mono.just("new-hash"));
    given(updateUserPasswordPort.updateUserPassword("user-id", "new-hash")).willReturn(Mono.empty());

    StepVerifier.create(sut.changePassword(null,
        new ChangePasswordCommand(null, "user-id.raw-token", "new-password")))
        .verifyComplete();

    verify(updateUserPasswordPort).updateUserPassword("user-id", "new-hash");
  }

  @Test
  @DisplayName("만료되거나 이미 소비한 재설정 token은 비밀번호를 저장하지 않는다")
  void changePassword_invalidResetTokenDoesNotUpdatePassword() {
    given(authTokenPort.consume(AuthTokenType.PASSWORD_RESET, "user-id", "raw-token"))
        .willReturn(Mono.just(AuthTokenConsumeResult.MISSING));

    StepVerifier.create(sut.changePassword(null,
        new ChangePasswordCommand(null, "user-id.raw-token", "new-password")))
        .expectErrorSatisfies(error -> assertThat(((DomainException) error).getErrorCode())
            .isEqualTo(UserErrorCode.PASSWORD_RESET_TOKEN_INVALID))
        .verify();

    verify(passwordHashPort, never()).hash(any());
    verify(updateUserPasswordPort, never()).updateUserPassword(any(), any());
  }

  @Test
  @DisplayName("로그인된 로컬 계정은 현재 비밀번호가 일치할 때만 변경한다")
  void changePassword_authenticatedLocalUserUpdatesAfterCurrentPasswordMatches() {
    User user = User.signUp("user-id", "user@example.com", "user", "old-hash");
    given(findUserByIdPort.findUserById("user-id")).willReturn(Mono.just(user));
    given(passwordHashPort.matches("current-password", "old-hash")).willReturn(Mono.just(true));
    given(passwordHashPort.hash("new-password")).willReturn(Mono.just("new-hash"));
    given(updateUserPasswordPort.updateUserPassword("user-id", "new-hash")).willReturn(Mono.empty());

    StepVerifier.create(sut.changePassword("user-id",
        new ChangePasswordCommand("current-password", null, "new-password")))
        .verifyComplete();

    verify(authTokenPort, never()).consume(any(), any(), any());
  }

  @Test
  @DisplayName("로그인된 OAuth 전용 계정은 비밀번호 변경을 거부한다")
  void changePassword_authenticatedOAuthOnlyUserIsRejected() {
    User user = User.signUpOAuth("user-id", "user@example.com", "user");
    given(findUserByIdPort.findUserById("user-id")).willReturn(Mono.just(user));

    StepVerifier.create(sut.changePassword("user-id",
        new ChangePasswordCommand("current-password", null, "new-password")))
        .expectErrorSatisfies(error -> assertThat(((DomainException) error).getErrorCode())
            .isEqualTo(UserErrorCode.PASSWORD_CHANGE_UNAVAILABLE))
        .verify();

    verify(updateUserPasswordPort, never()).updateUserPassword(any(), any());
  }

}
