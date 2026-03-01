package com.schemafy.domain.erd.memo.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeMemoPositionPort {

  Mono<Void> changeMemoPosition(String memoId, String positions);

}
