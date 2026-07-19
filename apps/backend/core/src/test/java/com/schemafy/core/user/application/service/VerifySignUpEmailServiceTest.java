package com.schemafy.core.user.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.VerifySignUpEmailCommand;
import com.schemafy.core.user.application.port.out.AuthMailPolicyPort;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.security.SignupVerificationTokenGenerator;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
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
@DisplayName("회원가입 이메일 인증 서비스")
class VerifySignUpEmailServiceTest {

  @Mock
  ExistsUserByEmailPort existsUserByEmailPort;

  @Mock
  AuthTokenPort authTokenPort;

  @Mock
  SignupVerificationTokenGenerator signupVerificationTokenGenerator;

  @Mock
  AuthMailPolicyPort authMailPolicyPort;

  @InjectMocks
  VerifySignUpEmailService sut;

  @BeforeEach
  void setUp() {
    given(authMailPolicyPort.isEnabled()).willReturn(true);
  }

  @Test
  @DisplayName("인증 코드가 맞으면 signup verification token을 발급한다")
  void verifySignUpEmail_success() {
    VerifySignUpEmailCommand command = new VerifySignUpEmailCommand(
        "test@example.com", "123456");

    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.consume(AuthTokenType.EMAIL_VERIFICATION,
        "test@example.com", "123456"))
        .willReturn(Mono.just(AuthTokenConsumeResult.CONSUMED));
    given(signupVerificationTokenGenerator.generate())
        .willReturn("signup-token");
    given(authTokenPort.save(any(AuthToken.class))).willReturn(Mono.empty());

    StepVerifier.create(sut.verifySignUpEmail(command))
        .assertNext(result -> {
          assertThat(result.email()).isEqualTo("test@example.com");
          assertThat(result.signupVerificationToken()).isEqualTo("signup-token");
          assertThat(result.expiresAt()).isNotNull();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("토큰이 없으면 만료 오류를 반환한다")
  void verifySignUpEmail_missingToken() {
    VerifySignUpEmailCommand command = new VerifySignUpEmailCommand(
        "test@example.com", "123456");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.consume(AuthTokenType.EMAIL_VERIFICATION,
        "test@example.com", "123456"))
        .willReturn(Mono.just(AuthTokenConsumeResult.MISSING));

    StepVerifier.create(sut.verifySignUpEmail(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.VERIFICATION_CODE_EXPIRED);
        })
        .verify();
  }

  @Test
  @DisplayName("코드가 틀리면 INVALID 오류를 반환한다")
  void verifySignUpEmail_mismatch() {
    VerifySignUpEmailCommand command = new VerifySignUpEmailCommand(
        "test@example.com", "123456");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.consume(AuthTokenType.EMAIL_VERIFICATION,
        "test@example.com", "123456"))
        .willReturn(Mono.just(AuthTokenConsumeResult.MISMATCH));

    StepVerifier.create(sut.verifySignUpEmail(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.VERIFICATION_CODE_INVALID);
        })
        .verify();
  }

  @Test
  @DisplayName("코드 시도 횟수를 초과하면 ATTEMPTS_EXCEEDED 오류를 반환한다")
  void verifySignUpEmail_attemptsExceeded() {
    VerifySignUpEmailCommand command = new VerifySignUpEmailCommand(
        "test@example.com", "123456");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.consume(AuthTokenType.EMAIL_VERIFICATION,
        "test@example.com", "123456"))
        .willReturn(Mono.just(AuthTokenConsumeResult.ATTEMPTS_EXCEEDED));

    StepVerifier.create(sut.verifySignUpEmail(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
        })
        .verify();
  }

  @Test
  @DisplayName("이미 가입된 이메일이면 ALREADY_EXISTS를 반환한다")
  void verifySignUpEmail_alreadyExists() {
    VerifySignUpEmailCommand command = new VerifySignUpEmailCommand(
        "test@example.com", "123456");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(true));

    StepVerifier.create(sut.verifySignUpEmail(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.ALREADY_EXISTS);
        })
        .verify();
  }

  @Test
  @DisplayName("인증 메일이 비활성화되면 토큰을 소비하지 않고 AUTH_MAIL_DISABLED를 반환한다")
  void verifySignUpEmail_mailDisabled() {
    given(authMailPolicyPort.isEnabled()).willReturn(false);

    StepVerifier.create(sut.verifySignUpEmail(
        new VerifySignUpEmailCommand("test@example.com", "123456")))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.AUTH_MAIL_DISABLED);
        })
        .verify();

    verify(existsUserByEmailPort, never()).existsUserByEmail(any());
    verify(authTokenPort, never()).consume(any(), any(), any());
    verify(signupVerificationTokenGenerator, never()).generate();
  }

}
