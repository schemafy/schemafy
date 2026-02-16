package com.schemafy.domain.erd.vendor.application.port.in;

import com.schemafy.domain.erd.vendor.domain.DbVendor;

import reactor.core.publisher.Mono;

public interface GetDbVendorUseCase {

  Mono<DbVendor> getDbVendor(GetDbVendorQuery query);

}
