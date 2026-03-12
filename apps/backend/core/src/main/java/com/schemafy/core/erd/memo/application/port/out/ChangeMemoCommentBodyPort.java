package com.schemafy.core.erd.memo.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeMemoCommentBodyPort {

  Mono<Void> changeMemoCommentBody(String commentId, String body);

}
