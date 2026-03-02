package com.schemafy.core.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.TestFixture;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.user.repository.UserAuthProviderRepository;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.service.dto.OAuthLoginCommand;
import com.schemafy.domain.user.domain.AuthProvider;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("UserService OAuth race 처리 테스트")
class UserServiceOAuthRaceHandlingTest {

  @Autowired
  UserService userService;

  @Autowired
  UserRepository userRepository;

  @Autowired
  UserAuthProviderRepository userAuthProviderRepository;

  @Autowired
  WorkspaceRepository workspaceRepository;

  @Autowired
  WorkspaceMemberRepository workspaceMemberRepository;

  @BeforeEach
  void setUp() {
    userAuthProviderRepository.deleteAll().block();
    workspaceMemberRepository.deleteAll().block();
    workspaceRepository.deleteAll().block();
    userRepository.deleteAll().block();
  }

  @Test
  @DisplayName("같은 이메일의 기존 유저가 있으면 OAuth provider를 자동 연동하고 기존 유저를 반환한다")
  void loginOrSignUpOAuth_linkExistingUser_success() {
    var existingUser = TestFixture
        .createTestUser("existing@example.com", "Existing User", "password")
        .flatMap(userRepository::save)
        .block();

    String providerUserId = "github-user-1";

    OAuthLoginCommand command = new OAuthLoginCommand(
        "existing@example.com",
        "Existing User",
        AuthProvider.GITHUB,
        providerUserId);

    StepVerifier.create(userService.loginOrSignUpOAuth(command))
        .assertNext(user -> {
          assertThat(user.id()).isEqualTo(existingUser.getId());
          assertThat(user.email()).isEqualTo(existingUser.getEmail());
        })
        .verifyComplete();

    StepVerifier.create(userAuthProviderRepository.findByProviderAndProviderUserId(
        AuthProvider.GITHUB.name(), providerUserId))
        .assertNext(provider -> assertThat(provider.getUserId()).isEqualTo(existingUser.getId()))
        .verifyComplete();
  }
}
