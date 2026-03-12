package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Flux;

public interface GetUsersByIdsUseCase {

  Flux<User> getUsersByIds(GetUsersByIdsQuery query);

}
