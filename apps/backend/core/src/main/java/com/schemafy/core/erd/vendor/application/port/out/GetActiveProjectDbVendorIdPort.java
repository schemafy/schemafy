package com.schemafy.core.erd.vendor.application.port.out;

import reactor.core.publisher.Mono;

public interface GetActiveProjectDbVendorIdPort {

  Mono<Integer> findDbVendorIdByProjectId(String projectId);

}
