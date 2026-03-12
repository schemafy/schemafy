package com.schemafy.core.user.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.core.user.application.port.in.GetUsersByIdsUseCase;
import com.schemafy.core.user.application.port.out.FindUsersByIdsPort;
import com.schemafy.core.user.domain.User;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
class GetUsersByIdsService implements GetUsersByIdsUseCase {

  private final FindUsersByIdsPort findUsersByIdsPort;

  @Override
  public Flux<User> getUsersByIds(GetUsersByIdsQuery query) {
    if (query.userIds() == null || query.userIds().isEmpty()) {
      return Flux.empty();
    }
    return findUsersByIdsPort.findUsersByIds(query.userIds());
  }

}
