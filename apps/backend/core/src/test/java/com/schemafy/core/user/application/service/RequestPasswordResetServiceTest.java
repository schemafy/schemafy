package com.schemafy.core.user.application.service;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.user.application.port.in.RequestPasswordResetCommand;
import com.schemafy.core.user.application.port.out.AuthMailPolicyPort;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.application.port.out.SendPasswordResetEmailPort;
import com.schemafy.core.user.application.security.SignupVerificationTokenGenerator;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("비밀번호 재설정 링크 발송 서비스")
class RequestPasswordResetServiceTest {

  @Mock
  AuthMailPolicyPort authMailPolicyPort;
  @Mock
  FindUserByEmailPort findUserByEmailPort;
  @Mock
  AuthTokenPort authTokenPort;
  @Mock
  SignupVerificationTokenGenerator tokenGenerator;
  @Mock
  SendPasswordResetEmailPort sendPasswordResetEmailPort;
  @InjectMocks
  RequestPasswordResetService sut;

  @BeforeEach
  void setUp() {
    given(authMailPolicyPort.isEnabled()).willReturn(true);
  }

  @Test
  @DisplayName("로컬 비밀번호 계정이면 Redis token을 저장하고 재설정 링크를 발송한다")
  void requestPasswordReset_localPasswordAccountIssuesTokenAndSendsLink() {
    User user = User.signUp("user-id", "user@example.com", "user", "hash");
    given(findUserByEmailPort.findUserByEmail(user.email())).willReturn(Mono.just(user));
    given(authTokenPort.findExpiresAt(AuthTokenType.PASSWORD_RESET, user.id())).willReturn(Mono.empty());
    given(tokenGenerator.generate()).willReturn("raw-token");
    given(authTokenPort.saveIfAbsent(any(AuthToken.class))).willReturn(Mono.just(true));
    given(sendPasswordResetEmailPort.sendPasswordResetLink(eq(user.email()), any(), any(Instant.class)))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.requestPasswordReset(new RequestPasswordResetCommand(user.email())))
        .verifyComplete();

    verify(sendPasswordResetEmailPort).sendPasswordResetLink(
        eq(user.email()), eq("user-id.raw-token"), any(Instant.class));
  }

  @Test
  @DisplayName("유효한 재설정 token이 있으면 새 링크를 발송하지 않는다")
  void requestPasswordReset_existingTokenDoesNotSendAnotherEmail() {
    User user = User.signUp("user-id", "user@example.com", "user", "hash");
    given(findUserByEmailPort.findUserByEmail(user.email())).willReturn(Mono.just(user));
    given(authTokenPort.findExpiresAt(AuthTokenType.PASSWORD_RESET, user.id()))
        .willReturn(Mono.just(Instant.now().plusSeconds(600)));

    StepVerifier.create(sut.requestPasswordReset(new RequestPasswordResetCommand(user.email())))
        .verifyComplete();

    verify(sendPasswordResetEmailPort, never()).sendPasswordResetLink(any(), any(), any());
  }

  @Test
  @DisplayName("메일 발송과 token 정리가 모두 실패해도 일반 성공으로 응답한다")
  void requestPasswordReset_deliveryAndCleanupFailuresReturnGenericSuccess() {
    User user = User.signUp("user-id", "user@example.com", "user", "hash");
    given(findUserByEmailPort.findUserByEmail(user.email())).willReturn(Mono.just(user));
    given(authTokenPort.findExpiresAt(AuthTokenType.PASSWORD_RESET, user.id())).willReturn(Mono.empty());
    given(tokenGenerator.generate()).willReturn("raw-token");
    given(authTokenPort.saveIfAbsent(any(AuthToken.class))).willReturn(Mono.just(true));
    given(sendPasswordResetEmailPort.sendPasswordResetLink(any(), any(), any()))
        .willReturn(Mono.error(new RuntimeException("delivery failed")));
    given(authTokenPort.delete(AuthTokenType.PASSWORD_RESET, user.id()))
        .willReturn(Mono.error(new RuntimeException("cleanup failed")));

    StepVerifier.create(sut.requestPasswordReset(new RequestPasswordResetCommand(user.email())))
        .verifyComplete();
  }

}
