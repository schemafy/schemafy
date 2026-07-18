package com.schemafy.core.user.application.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.application.port.in.SignUpUserCommand;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.CreateUserPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.port.out.PasswordHashPort;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 유저 서비스")
class SignUpUserServiceTest {

  @Mock
  ExistsUserByEmailPort existsUserByEmailPort;

  @Mock
  PasswordHashPort passwordHashPort;

  @Mock
  CreateUserPort createUserPort;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  AuthTokenPort authTokenPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  SignUpUserService sut;

  @BeforeEach
  void setUp() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("이메일 인증 토큰이 유효하면 ACTIVE 유저를 생성한다")
  void signUpUser_success() {
    SignUpUserCommand command = command("test@example.com", "signup-token");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.consume(AuthTokenType.SIGNUP_VERIFICATION,
        "test@example.com", "signup-token"))
        .willReturn(Mono.just(AuthTokenConsumeResult.CONSUMED));
    given(ulidGeneratorPort.generate()).willReturn("user-1");
    given(passwordHashPort.hash("password")).willReturn(Mono.just("encoded"));
    given(createUserPort.createUser(any(User.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0, User.class)));

    StepVerifier.create(sut.signUpUser(command))
        .assertNext(user -> {
          assertThat(user.id()).isEqualTo("user-1");
          assertThat(user.email()).isEqualTo("test@example.com");
          assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("이미 가입된 이메일이면 ALREADY_EXISTS를 반환한다")
  void signUpUser_alreadyExists() {
    SignUpUserCommand command = command("test@example.com", "signup-token");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(true));

    StepVerifier.create(sut.signUpUser(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.ALREADY_EXISTS);
        })
        .verify();
  }

  @Test
  @DisplayName("이메일 인증 토큰이 없으면 EMAIL_NOT_VERIFIED를 반환한다")
  void signUpUser_missingSignupVerification() {
    SignUpUserCommand command = command("test@example.com", "signup-token");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.consume(AuthTokenType.SIGNUP_VERIFICATION,
        "test@example.com", "signup-token"))
        .willReturn(Mono.just(AuthTokenConsumeResult.MISSING));

    StepVerifier.create(sut.signUpUser(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.EMAIL_NOT_VERIFIED);
        })
        .verify();
  }

  @Test
  @DisplayName("회원가입 저장 시 unique 충돌이면 ALREADY_EXISTS를 반환한다")
  void signUpUser_duplicateKey_mapsAlreadyExists() {
    SignUpUserCommand command = command("test@example.com", "signup-token");
    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(authTokenPort.consume(AuthTokenType.SIGNUP_VERIFICATION,
        "test@example.com", "signup-token"))
        .willReturn(Mono.just(AuthTokenConsumeResult.CONSUMED));
    given(ulidGeneratorPort.generate()).willReturn("user-1");
    given(passwordHashPort.hash("password")).willReturn(Mono.just("encoded"));
    given(createUserPort.createUser(any(User.class)))
        .willReturn(Mono.error(new DuplicateKeyException("duplicate")));

    StepVerifier.create(sut.signUpUser(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.ALREADY_EXISTS);
        })
        .verify();
  }

  private SignUpUserCommand command(String email, String token) {
    return new SignUpUserCommand(email, "Tester", "password", token);
  }

}
