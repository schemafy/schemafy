package com.schemafy.core.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.schemafy.core.common.TestFixture;
import com.schemafy.core.user.exception.UserErrorCode;
import com.schemafy.core.user.repository.UserAuthProviderRepository;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.entity.UserAuthProvider;
import com.schemafy.core.user.repository.vo.AuthProvider;
import com.schemafy.core.user.service.dto.OAuthLoginCommand;
import com.schemafy.domain.common.exception.DomainException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("UserService OAuth race 처리 테스트")
class UserServiceOAuthRaceHandlingTest {

  @Autowired
  UserService userService;

  @MockitoSpyBean
  UserRepository userRepository;

  @MockitoSpyBean
  UserAuthProviderRepository userAuthProviderRepository;

  @BeforeEach
  void setUp() {
    Mockito.reset(userRepository, userAuthProviderRepository);
    userAuthProviderRepository.deleteAll().block();
    userRepository.deleteAll().block();
  }

  @Test
  @DisplayName("기존 이메일 자동 연동 중 provider 중복이면 재조회로 성공 처리한다")
  void loginOrSignUpOAuth_providerDuplicateDuringAutoLink_returnsLinkedUser() {
    User existingUser = TestFixture
        .createTestUser("existing@example.com", "Existing User", "password")
        .flatMap(userRepository::save)
        .block();

    String providerUserId = "github-user-1";
    UserAuthProvider linkedProvider = UserAuthProvider.create(
        existingUser.getId(),
        AuthProvider.GITHUB,
        providerUserId);

    doReturn(
        Mono.empty(),
        Mono.just(linkedProvider))
        .when(userAuthProviderRepository)
        .findByProviderAndProviderUserId("GITHUB", providerUserId);
    doReturn(Mono.error(new DuplicateKeyException("duplicate provider")))
        .when(userAuthProviderRepository)
        .save(any(UserAuthProvider.class));

    OAuthLoginCommand command = new OAuthLoginCommand(
        "existing@example.com",
        "Existing User",
        AuthProvider.GITHUB,
        providerUserId);

    StepVerifier.create(userService.loginOrSignUpOAuth(command))
        .assertNext(user -> {
          assertThat(user.getId()).isEqualTo(existingUser.getId());
          assertThat(user.getEmail()).isEqualTo(existingUser.getEmail());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("OAuth 신규 유저 생성 중 이메일 중복이면 USER_ALREADY_EXISTS로 매핑한다")
  void loginOrSignUpOAuth_emailDuplicateDuringCreate_mapsToUserAlreadyExists() {
    String email = "oauth-race@example.com";
    String providerUserId = "github-user-2";

    doReturn(Mono.empty())
        .when(userAuthProviderRepository)
        .findByProviderAndProviderUserId("GITHUB", providerUserId);
    doReturn(Mono.empty())
        .when(userRepository)
        .findByEmail(email);
    doReturn(Mono.error(new DuplicateKeyException("duplicate email")))
        .when(userRepository)
        .save(any(User.class));

    OAuthLoginCommand command = new OAuthLoginCommand(
        email,
        "OAuth Race User",
        AuthProvider.GITHUB,
        providerUserId);

    StepVerifier.create(userService.loginOrSignUpOAuth(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          DomainException domainException = (DomainException) error;
          assertThat(domainException.getErrorCode())
              .isEqualTo(UserErrorCode.ALREADY_EXISTS);
        })
        .verify();
  }

}
