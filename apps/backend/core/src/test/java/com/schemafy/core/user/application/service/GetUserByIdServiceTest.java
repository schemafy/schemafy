package com.schemafy.core.user.application.service;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.GetUserByIdQuery;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserByIdService")
class GetUserByIdServiceTest {

  @Mock
  FindUserByIdPort findUserByIdPort;

  @InjectMocks
  GetUserByIdService sut;

  @Test
  @DisplayName("getUserById: 유저를 조회한다")
  void getUserById_success() {
    User user = new User(
        "user-1",
        "test@example.com",
        "Tester",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);

    given(findUserByIdPort.findUserById("user-1"))
        .willReturn(Mono.just(user));

    StepVerifier.create(sut.getUserById(new GetUserByIdQuery("user-1")))
        .assertNext(found -> assertThat(found.email()).isEqualTo("test@example.com"))
        .verifyComplete();
  }

  @Test
  @DisplayName("getUserById: ACTIVE 상태가 아니면 ACCOUNT_NOT_ACTIVE")
  void getUserById_inactiveUser() {
    User user = new User(
        "user-1",
        "test@example.com",
        "Tester",
        "encoded",
        UserStatus.INACTIVE,
        null,
        null,
        null);

    given(findUserByIdPort.findUserById("user-1"))
        .willReturn(Mono.just(user));

    StepVerifier.create(sut.getUserById(new GetUserByIdQuery("user-1")))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.ACCOUNT_NOT_ACTIVE);
        })
        .verify();
  }

  @Test
  @DisplayName("getUserById: 삭제된 계정이면 ACCOUNT_NOT_ACTIVE")
  void getUserById_deletedUser() {
    User user = new User(
        "user-1",
        "test@example.com",
        "Tester",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z"));

    given(findUserByIdPort.findUserById("user-1"))
        .willReturn(Mono.just(user));

    StepVerifier.create(sut.getUserById(new GetUserByIdQuery("user-1")))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.ACCOUNT_NOT_ACTIVE);
        })
        .verify();
  }

  @Test
  @DisplayName("getUserById: 유저가 없으면 NOT_FOUND")
  void getUserById_notFound() {
    given(findUserByIdPort.findUserById("none"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.getUserById(new GetUserByIdQuery("none")))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.NOT_FOUND);
        })
        .verify();
  }

}
