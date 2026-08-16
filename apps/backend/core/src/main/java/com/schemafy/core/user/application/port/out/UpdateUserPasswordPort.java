package com.schemafy.core.user.application.port.out;

import reactor.core.publisher.Mono;

public interface UpdateUserPasswordPort {

  Mono<Void> updateUserPassword(String userId, String encodedPassword);

}
