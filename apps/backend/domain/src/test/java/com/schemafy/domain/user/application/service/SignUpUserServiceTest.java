package com.schemafy.domain.user.application.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.domain.user.application.port.in.SignUpUserCommand;
import com.schemafy.domain.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.domain.user.application.port.out.PasswordHashPort;
import com.schemafy.domain.user.application.port.out.CreateUserPort;
import com.schemafy.domain.user.domain.User;
import com.schemafy.domain.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignUpUserService")
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
  TransactionalOperator transactionalOperator;

  @InjectMocks
  SignUpUserService sut;

  @BeforeEach
  void setUp() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("signUpUser: 신규 유저를 생성한다")
  void signUpUser_success() {
    SignUpUserCommand command = new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "password");

    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
    given(ulidGeneratorPort.generate()).willReturn("user-1");
    given(passwordHashPort.hash("password")).willReturn(Mono.just("encoded"));
    given(createUserPort.createUser(any(User.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0, User.class)));

    StepVerifier.create(sut.signUpUser(command))
        .assertNext(user -> {
          assertThat(user.id()).isEqualTo("user-1");
          assertThat(user.email()).isEqualTo("test@example.com");
          assertThat(user.name()).isEqualTo("Tester");
          assertThat(user.password()).isEqualTo("encoded");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("signUpUser: 이미 존재하는 이메일이면 ALREADY_EXISTS를 반환한다")
  void signUpUser_alreadyExists() {
    SignUpUserCommand command = new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "password");

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
  @DisplayName("signUpUser: 저장 시 unique 충돌이면 ALREADY_EXISTS로 매핑한다")
  void signUpUser_duplicateKey_mapsAlreadyExists() {
    SignUpUserCommand command = new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "password");

    given(existsUserByEmailPort.existsUserByEmail("test@example.com"))
        .willReturn(Mono.just(false));
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

}
