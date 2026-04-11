package com.schemafy.core.user.adapter.out.persistence;

import java.time.Instant;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.config.R2dbcTestConfiguration;
import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserAuthProvider;
import com.schemafy.core.user.domain.UserStatus;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
  UserPersistenceAdapter.class,
  UserAuthProviderPersistenceAdapter.class,
  UserMapper.class,
  UserAuthProviderMapper.class,
  R2dbcTestConfiguration.class })
@DisplayName("UserPersistenceAdapter")
class UserPersistenceAdapterTest {

  @Autowired
  UserPersistenceAdapter userPersistenceAdapter;

  @Autowired
  UserAuthProviderPersistenceAdapter userAuthProviderPersistenceAdapter;

  @Autowired
  UserRepository userRepository;

  @Autowired
  UserAuthProviderRepository userAuthProviderRepository;

  @BeforeEach
  void setUp() {
    userAuthProviderRepository.deleteAll().block();
    userRepository.deleteAll().block();
  }

  @Nested
  @DisplayName("User persistence")
  class UserPersistence {

    @Test
    @DisplayName("saveUser/findUserById/findUserByEmail: 유저를 저장하고 조회한다")
    void saveAndFindUser() {
      User user = new User(
          "06D8A000000000000000200001",
          "user@example.com",
          "User",
          "encoded-password",
          UserStatus.ACTIVE,
          null,
          null,
          null);

      StepVerifier.create(userPersistenceAdapter.createUser(user))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(user.id());
            assertThat(saved.email()).isEqualTo(user.email());
            assertThat(saved.createdAt()).isNotNull();
            assertThat(saved.updatedAt()).isNotNull();
          })
          .verifyComplete();

      StepVerifier.create(userPersistenceAdapter.findUserById(user.id()))
          .assertNext(found -> assertThat(found.email()).isEqualTo(user.email()))
          .verifyComplete();

      StepVerifier.create(userPersistenceAdapter.findUserByEmail(user.email()))
          .assertNext(found -> assertThat(found.id()).isEqualTo(user.id()))
          .verifyComplete();
    }

    @Test
    @DisplayName("existsUserByEmail: 이메일 존재 여부를 반환한다")
    void existsUserByEmail() {
      User user = new User(
          "06D8A000000000000000200002",
          "exists@example.com",
          "Exists",
          "encoded-password",
          UserStatus.ACTIVE,
          null,
          null,
          null);

      userPersistenceAdapter.createUser(user).block();

      StepVerifier.create(userPersistenceAdapter.existsUserByEmail("exists@example.com"))
          .expectNext(true)
          .verifyComplete();

      StepVerifier.create(userPersistenceAdapter.existsUserByEmail("missing@example.com"))
          .expectNext(false)
          .verifyComplete();
    }

    @Test
    @DisplayName("findUserByEmail: deleted_at이 있어도 조회된다")
    void findUserByEmail_includesDeletedUser() {
      Instant deletedAt = Instant.parse("2026-01-01T00:00:00Z");
      User user = new User(
          "06D8A000000000000000200003",
          "deleted@example.com",
          "Deleted",
          "encoded-password",
          UserStatus.ACTIVE,
          null,
          null,
          deletedAt);

      userPersistenceAdapter.createUser(user).block();

      StepVerifier.create(userPersistenceAdapter.findUserByEmail("deleted@example.com"))
          .assertNext(found -> assertThat(found.deletedAt()).isEqualTo(deletedAt))
          .verifyComplete();
    }

    @Test
    @DisplayName("saveUser: 이메일 unique 충돌 시 예외가 발생한다")
    void saveUser_duplicateEmail_throws() {
      User first = new User(
          "06D8A000000000000000200004",
          "dup@example.com",
          "First",
          "encoded-password",
          UserStatus.ACTIVE,
          null,
          null,
          null);
      User second = new User(
          "06D8A000000000000000200005",
          "dup@example.com",
          "Second",
          "encoded-password",
          UserStatus.ACTIVE,
          null,
          null,
          null);

      userPersistenceAdapter.createUser(first).block();

      StepVerifier.create(userPersistenceAdapter.createUser(second))
          .expectErrorSatisfies(error -> assertThat(error)
              .isInstanceOfAny(DuplicateKeyException.class, DataIntegrityViolationException.class))
          .verify();
    }

    @Test
    @DisplayName("findUsersByIds: ID 집합 기반으로 조회하고 미존재 ID는 제외한다")
    void findUsersByIds_filtersMissingId() {
      User active = new User(
          "06D8A000000000000000200012",
          "active@example.com",
          "Active",
          "encoded-password",
          UserStatus.ACTIVE,
          null,
          null,
          null);
      User deleted = new User(
          "06D8A000000000000000200013",
          "deleted-users@example.com",
          "Deleted",
          "encoded-password",
          UserStatus.ACTIVE,
          null,
          null,
          Instant.parse("2026-01-01T00:00:00Z"));
      userPersistenceAdapter.createUser(active).block();
      userPersistenceAdapter.createUser(deleted).block();

      StepVerifier.create(
          userPersistenceAdapter.findUsersByIds(Set.of(active.id(), deleted.id(), "missing"))
              .map(User::id)
              .collectList())
          .assertNext(ids -> {
            assertThat(ids).contains(active.id(), deleted.id());
            assertThat(ids).doesNotContain("missing");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("findUsersByIds: 빈 집합이면 empty를 반환한다")
    void findUsersByIds_emptySet_returnsEmpty() {
      StepVerifier.create(userPersistenceAdapter.findUsersByIds(Set.of()))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("UserAuthProvider persistence")
  class UserAuthProviderPersistence {

    @Test
    @DisplayName("save/find: provider와 providerUserId로 연동 정보를 조회한다")
    void saveAndFindUserAuthProvider() {
      User user = new User(
          "06D8A000000000000000200006",
          "oauth@example.com",
          "OAuth User",
          null,
          UserStatus.ACTIVE,
          null,
          null,
          null);
      userPersistenceAdapter.createUser(user).block();

      UserAuthProvider authProvider = new UserAuthProvider(
          "06D8A000000000000000200007",
          user.id(),
          AuthProvider.GITHUB,
          "github-user-1",
          null,
          null,
          null);

      StepVerifier.create(userAuthProviderPersistenceAdapter.createUserAuthProvider(authProvider))
          .assertNext(saved -> assertThat(saved.id()).isEqualTo(authProvider.id()))
          .verifyComplete();

      StepVerifier.create(
          userAuthProviderPersistenceAdapter.findUserAuthProvider(
              AuthProvider.GITHUB, "github-user-1"))
          .assertNext(found -> {
            assertThat(found.userId()).isEqualTo(user.id());
            assertThat(found.provider()).isEqualTo(AuthProvider.GITHUB);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("saveUserAuthProvider: provider/providerUserId unique 충돌 시 예외가 발생한다")
    void saveUserAuthProvider_duplicateProviderUserId_throws() {
      User user1 = new User(
          "06D8A000000000000000200008",
          "provider-1@example.com",
          "Provider1",
          null,
          UserStatus.ACTIVE,
          null,
          null,
          null);
      User user2 = new User(
          "06D8A000000000000000200009",
          "provider-2@example.com",
          "Provider2",
          null,
          UserStatus.ACTIVE,
          null,
          null,
          null);
      userPersistenceAdapter.createUser(user1).block();
      userPersistenceAdapter.createUser(user2).block();

      UserAuthProvider first = new UserAuthProvider(
          "06D8A000000000000000200010",
          user1.id(),
          AuthProvider.GITHUB,
          "github-user-dup",
          null,
          null,
          null);
      UserAuthProvider second = new UserAuthProvider(
          "06D8A000000000000000200011",
          user2.id(),
          AuthProvider.GITHUB,
          "github-user-dup",
          null,
          null,
          null);

      userAuthProviderPersistenceAdapter.createUserAuthProvider(first).block();

      StepVerifier.create(userAuthProviderPersistenceAdapter.createUserAuthProvider(second))
          .expectErrorSatisfies(error -> assertThat(error)
              .isInstanceOfAny(DuplicateKeyException.class, DataIntegrityViolationException.class))
          .verify();
    }

  }

}
