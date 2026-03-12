package com.schemafy.core.user.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.GetUserByIdQuery;
import com.schemafy.core.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetUserByIdService implements GetUserByIdUseCase {

  private final FindUserByIdPort findUserByIdPort;

  @Override
  public Mono<User> getUserById(GetUserByIdQuery query) {
    return findUserByIdPort.findUserById(query.userId())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)));
  }

}
