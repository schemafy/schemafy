package com.schemafy.core.erd.vendor.application.port.in;

import com.schemafy.core.erd.vendor.domain.DbVendor;

import reactor.core.publisher.Mono;

public interface GetProjectDbVendorUseCase {

  Mono<DbVendor> getProjectDbVendor(GetProjectDbVendorQuery query);

}
