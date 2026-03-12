package com.schemafy.core.erd.vendor.application.port.out;

import com.schemafy.core.erd.vendor.domain.DbVendor;

import reactor.core.publisher.Mono;

public interface GetDbVendorByDisplayNamePort {

  Mono<DbVendor> findByDisplayName(String displayName);

}
