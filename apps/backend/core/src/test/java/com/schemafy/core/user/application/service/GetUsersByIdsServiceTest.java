package com.schemafy.core.user.application.service;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.core.user.application.port.out.FindUsersByIdsPort;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUsersByIdsService")
class GetUsersByIdsServiceTest {

  @Mock
  FindUsersByIdsPort findUsersByIdsPort;

  @InjectMocks
  GetUsersByIdsService sut;

  @Test
  @DisplayName("getUsersByIds: 빈 집합이면 empty를 반환한다")
  void getUsersByIds_emptySet_returnsEmpty() {
    StepVerifier.create(sut.getUsersByIds(new GetUsersByIdsQuery(Set.of())))
        .verifyComplete();

    verifyNoInteractions(findUsersByIdsPort);
  }

  @Test
  @DisplayName("getUsersByIds: 다건 조회 성공")
  void getUsersByIds_success() {
    User user1 = new User(
        "user-1",
        "u1@example.com",
        "User1",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);
    User user2 = new User(
        "user-2",
        "u2@example.com",
        "User2",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);
    Set<String> userIds = Set.of("user-1", "user-2");

    given(findUsersByIdsPort.findUsersByIds(userIds))
        .willReturn(Flux.just(user1, user2));

    StepVerifier.create(sut.getUsersByIds(new GetUsersByIdsQuery(userIds)))
        .assertNext(found -> assertThat(found.id()).isEqualTo("user-1"))
        .assertNext(found -> assertThat(found.id()).isEqualTo("user-2"))
        .verifyComplete();
  }

  @Test
  @DisplayName("getUsersByIds: 일부 ID가 없어도 조회된 유저만 반환한다")
  void getUsersByIds_partialSuccess() {
    User user1 = new User(
        "user-1",
        "u1@example.com",
        "User1",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);
    Set<String> userIds = Set.of("user-1", "missing");

    given(findUsersByIdsPort.findUsersByIds(userIds))
        .willReturn(Flux.just(user1));

    StepVerifier.create(sut.getUsersByIds(new GetUsersByIdsQuery(userIds)))
        .assertNext(found -> assertThat(found.id()).isEqualTo("user-1"))
        .verifyComplete();
  }

}
