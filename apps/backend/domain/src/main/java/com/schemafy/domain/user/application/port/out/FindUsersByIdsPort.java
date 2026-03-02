package com.schemafy.domain.user.application.port.out;

import java.util.Set;

import com.schemafy.domain.user.domain.User;

import reactor.core.publisher.Flux;

public interface FindUsersByIdsPort {

  Flux<User> findUsersByIds(Set<String> userIds);

}
