package com.schemafy.core.user.application.service;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.SendSignUpEmailCodeCommand;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.port.out.SendEmailVerificationPort;
import com.schemafy.core.user.application.security.VerificationCodeGenerator;
import com.schemafy.core.user.domain.AuthPolicy;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 이메일 인증 코드 발송 서비스")
class SendSignUpEmailCodeServiceTest {

  @Mock
  ExistsUserByEmailPort existsUserByEmailPort;

  @Mock
  VerificationCodeGenerator verificationCodeGenerator;

  @Mock
  AuthTokenPort authTokenPort;

  @Mock
  SendEmailVerificationPort sendEmailVerificationPort;

  @InjectMocks
  SendSignUpEmailCodeService sut;

  @Test
  @DisplayName("가입되지 않은 이메일이면 Redis 토큰 저장 후 인증 메일을 발송한다")
  void sendSignUpEmailCode_success() {
    SendSignUpEmailCodeCommand command = new SendSignUpEmailCodeCommand(
        "test@example.com");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, "test@example.com"))
        .willReturn(Mono.empty());
    given(verificationCodeGenerator.generate()).willReturn("123456");
    given(authTokenPort.saveIfAbsent(any(AuthToken.class))).willReturn(Mono.just(true));
    given(sendEmailVerificationPort.sendVerificationCode(
        any(), any(), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.sendSignUpEmailCode(command))
        .assertNext(result -> {
          assertThat(result.email()).isEqualTo("test@example.com");
          assertThat(result.expiresAt()).isAfter(Instant.now());
        })
        .verifyComplete();

    ArgumentCaptor<AuthToken> tokenCaptor = ArgumentCaptor.forClass(
        AuthToken.class);
    verify(authTokenPort).saveIfAbsent(tokenCaptor.capture());
    AuthToken token = tokenCaptor.getValue();
    assertThat(token.tokenType()).isEqualTo(AuthTokenType.EMAIL_VERIFICATION);
    assertThat(token.subject()).isEqualTo("test@example.com");
    assertThat(token.token()).isEqualTo("123456");
    assertThat(token.maxAttemptCount())
        .isEqualTo(AuthPolicy.EMAIL_VERIFICATION_MAX_ATTEMPTS);
    verify(sendEmailVerificationPort).sendVerificationCode(
        "test@example.com", "123456", token.expiresAt());
  }

  @Test
  @DisplayName("기존 인증 코드가 유효하면 같은 challenge를 반환하고 메일을 재발송하지 않는다")
  void sendSignUpEmailCode_existingTokenSkipsResend() {
    SendSignUpEmailCodeCommand command = new SendSignUpEmailCodeCommand(
        "test@example.com");
    Instant expiresAt = Instant.now().plusSeconds(120);
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, "test@example.com"))
        .willReturn(Mono.just(expiresAt));

    StepVerifier.create(sut.sendSignUpEmailCode(command))
        .assertNext(result -> {
          assertThat(result.email()).isEqualTo("test@example.com");
          assertThat(result.expiresAt()).isEqualTo(expiresAt);
        })
        .verifyComplete();

    verify(verificationCodeGenerator, never()).generate();
    verify(authTokenPort, never()).save(any(AuthToken.class));
    verify(authTokenPort, never()).saveIfAbsent(any(AuthToken.class));
    verify(sendEmailVerificationPort, never()).sendVerificationCode(any(), any(), any());
  }

  @Test
  @DisplayName("동시 요청으로 토큰 선점에 실패하면 기존 challenge를 반환하고 메일을 발송하지 않는다")
  void sendSignUpEmailCode_saveIfAbsentLostSkipsResend() {
    SendSignUpEmailCodeCommand command = new SendSignUpEmailCodeCommand(
        "test@example.com");
    Instant expiresAt = Instant.now().plusSeconds(120);
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, "test@example.com"))
        .willReturn(Mono.empty(), Mono.just(expiresAt));
    given(verificationCodeGenerator.generate()).willReturn("123456");
    given(authTokenPort.saveIfAbsent(any(AuthToken.class))).willReturn(Mono.just(false));

    StepVerifier.create(sut.sendSignUpEmailCode(command))
        .assertNext(result -> {
          assertThat(result.email()).isEqualTo("test@example.com");
          assertThat(result.expiresAt()).isEqualTo(expiresAt);
        })
        .verifyComplete();

    verify(sendEmailVerificationPort, never()).sendVerificationCode(any(), any(), any());
  }

  @Test
  @DisplayName("새 토큰 저장 후 메일 발송이 실패하면 저장한 토큰을 삭제한다")
  void sendSignUpEmailCode_mailFailureDeletesSavedToken() {
    SendSignUpEmailCodeCommand command = new SendSignUpEmailCodeCommand(
        "test@example.com");
    RuntimeException mailFailure = new RuntimeException("mail failed");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, "test@example.com"))
        .willReturn(Mono.empty());
    given(verificationCodeGenerator.generate()).willReturn("123456");
    given(authTokenPort.saveIfAbsent(any(AuthToken.class))).willReturn(Mono.just(true));
    given(sendEmailVerificationPort.sendVerificationCode(any(), any(), any()))
        .willReturn(Mono.error(mailFailure));
    given(authTokenPort.delete(AuthTokenType.EMAIL_VERIFICATION, "test@example.com"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.sendSignUpEmailCode(command))
        .expectErrorMatches(error -> error == mailFailure)
        .verify();

    verify(authTokenPort).delete(AuthTokenType.EMAIL_VERIFICATION, "test@example.com");
  }

  @Test
  @DisplayName("이미 가입된 이메일이면 ALREADY_EXISTS를 반환한다")
  void sendSignUpEmailCode_alreadyExists() {
    SendSignUpEmailCodeCommand command = new SendSignUpEmailCodeCommand(
        "test@example.com");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(true));

    StepVerifier.create(sut.sendSignUpEmailCode(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.ALREADY_EXISTS);
        })
        .verify();
  }

}
