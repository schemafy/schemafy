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
import com.schemafy.core.user.application.port.in.LoginOrSignUpOAuthCommand;
import com.schemafy.core.user.application.port.out.CreateUserAuthProviderPort;
import com.schemafy.core.user.application.port.out.CreateUserPort;
import com.schemafy.core.user.application.port.out.FindUserAuthProviderPort;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserAuthProvider;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth 로그인 또는 회원가입 서비스")
class LoginOrSignUpOAuthServiceTest {

  @Mock
  FindUserAuthProviderPort findUserAuthProviderPort;

  @Mock
  FindUserByEmailPort findUserByEmailPort;

  @Mock
  FindUserByIdPort findUserByIdPort;

  @Mock
  CreateUserPort createUserPort;

  @Mock
  CreateUserAuthProviderPort createUserAuthProviderPort;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  LoginOrSignUpOAuthService sut;

  @BeforeEach
  void setUp() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("OAuth 로그인 또는 회원가입 시 신규 유저를 생성한다")
  void loginOrSignUpOAuth_newUser() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "oauth@example.com",
        "OAuth User",
        AuthProvider.GITHUB,
        "github-1");

    User savedUser = new User(
        "user-1",
        "oauth@example.com",
        "OAuth User",
        null,
        UserStatus.ACTIVE,
        null,
        null,
        null);

    given(findUserAuthProviderPort.findUserAuthProvider(AuthProvider.GITHUB, "github-1"))
        .willReturn(Mono.<UserAuthProvider>empty());
    given(findUserByEmailPort.findUserByEmail("oauth@example.com"))
        .willReturn(Mono.<User>empty());
    given(ulidGeneratorPort.generate()).willReturn("user-1", "provider-1");
    given(createUserPort.createUser(any(User.class))).willReturn(Mono.just(savedUser));
    given(createUserAuthProviderPort.createUserAuthProvider(any(UserAuthProvider.class)))
        .willAnswer(
            invocation -> Mono.just(invocation.getArgument(0, UserAuthProvider.class)));

    StepVerifier.create(sut.loginOrSignUpOAuth(command))
        .assertNext(result -> {
          assertThat(result.newUser()).isTrue();
          assertThat(result.user().id()).isEqualTo("user-1");
          assertThat(result.user().email()).isEqualTo("oauth@example.com");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("OAuth 로그인 또는 회원가입 시 같은 이메일의 기존 유저를 자동 연동한다")
  void loginOrSignUpOAuth_linkExistingUser() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "existing@example.com",
        "Existing User",
        AuthProvider.GITHUB,
        "github-2");

    User existing = new User(
        "user-1",
        "existing@example.com",
        "Existing User",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);

    given(findUserAuthProviderPort.findUserAuthProvider(AuthProvider.GITHUB, "github-2"))
        .willReturn(Mono.<UserAuthProvider>empty());
    given(findUserByEmailPort.findUserByEmail("existing@example.com"))
        .willReturn(Mono.just(existing));
    given(ulidGeneratorPort.generate()).willReturn("provider-2");
    given(createUserAuthProviderPort.createUserAuthProvider(any(UserAuthProvider.class)))
        .willAnswer(
            invocation -> Mono.just(invocation.getArgument(0, UserAuthProvider.class)));

    StepVerifier.create(sut.loginOrSignUpOAuth(command))
        .assertNext(result -> {
          assertThat(result.newUser()).isFalse();
          assertThat(result.user().id()).isEqualTo("user-1");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("OAuth 로그인 또는 회원가입 시 대문자 이메일도 기존 유저에 자동 연동한다")
  void loginOrSignUpOAuth_linkExistingUser_caseInsensitiveEmail() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "EXISTING@EXAMPLE.COM",
        "Existing User",
        AuthProvider.GITHUB,
        "github-case");

    User existing = new User(
        "user-1",
        "existing@example.com",
        "Existing User",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);

    given(findUserAuthProviderPort.findUserAuthProvider(AuthProvider.GITHUB, "github-case"))
        .willReturn(Mono.<UserAuthProvider>empty());
    given(findUserByEmailPort.findUserByEmail("existing@example.com"))
        .willReturn(Mono.just(existing));
    given(ulidGeneratorPort.generate()).willReturn("provider-case");
    given(createUserAuthProviderPort.createUserAuthProvider(any(UserAuthProvider.class)))
        .willAnswer(
            invocation -> Mono.just(invocation.getArgument(0, UserAuthProvider.class)));

    StepVerifier.create(sut.loginOrSignUpOAuth(command))
        .assertNext(result -> {
          assertThat(result.newUser()).isFalse();
          assertThat(result.user().id()).isEqualTo("user-1");
          assertThat(result.user().email()).isEqualTo("existing@example.com");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("OAuth 자동 연동 중 provider 중복이면 재조회로 복구한다")
  void loginOrSignUpOAuth_duplicateProviderDuringAutoLink_resolvesByReread() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "existing@example.com",
        "Existing User",
        AuthProvider.GITHUB,
        "github-3");

    User existing = new User(
        "user-1",
        "existing@example.com",
        "Existing User",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);

    UserAuthProvider linked = new UserAuthProvider(
        "provider-3",
        "user-1",
        AuthProvider.GITHUB,
        "github-3",
        null,
        null,
        null);

    given(findUserAuthProviderPort.findUserAuthProvider(AuthProvider.GITHUB, "github-3"))
        .willReturn(Mono.<UserAuthProvider>empty())
        .willReturn(Mono.just(linked));
    given(findUserByEmailPort.findUserByEmail("existing@example.com"))
        .willReturn(Mono.just(existing));
    given(ulidGeneratorPort.generate()).willReturn("provider-3");
    given(createUserAuthProviderPort.createUserAuthProvider(any(UserAuthProvider.class)))
        .willReturn(Mono.error(new DuplicateKeyException("duplicate")));
    given(findUserByIdPort.findUserById("user-1"))
        .willReturn(Mono.just(existing));

    StepVerifier.create(sut.loginOrSignUpOAuth(command))
        .assertNext(result -> {
          assertThat(result.newUser()).isFalse();
          assertThat(result.user().id()).isEqualTo("user-1");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("OAuth 유저 생성 중 이메일이 중복이면 ALREADY_EXISTS를 반환한다")
  void loginOrSignUpOAuth_duplicateEmailDuringCreate_mapsAlreadyExists() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "oauth@example.com",
        "OAuth User",
        AuthProvider.GITHUB,
        "github-4");

    given(findUserAuthProviderPort.findUserAuthProvider(AuthProvider.GITHUB, "github-4"))
        .willReturn(Mono.<UserAuthProvider>empty());
    given(findUserByEmailPort.findUserByEmail("oauth@example.com"))
        .willReturn(Mono.<User>empty());
    given(ulidGeneratorPort.generate()).willReturn("user-2");
    given(createUserPort.createUser(any(User.class)))
        .willReturn(Mono.error(new DuplicateKeyException("duplicate email")));

    StepVerifier.create(sut.loginOrSignUpOAuth(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.ALREADY_EXISTS);
        })
        .verify();
  }

}
