package com.schemafy.domain.user.application.port.in;

import com.schemafy.domain.user.domain.User;

import reactor.core.publisher.Flux;

public interface GetUsersByIdsUseCase {

  Flux<User> getUsersByIds(GetUsersByIdsQuery query);

}
