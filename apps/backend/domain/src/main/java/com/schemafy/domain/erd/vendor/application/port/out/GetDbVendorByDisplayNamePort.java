package com.schemafy.domain.erd.vendor.application.port.out;

import com.schemafy.domain.erd.vendor.domain.DbVendor;

import reactor.core.publisher.Mono;

public interface GetDbVendorByDisplayNamePort {

  Mono<DbVendor> findByDisplayName(String displayName);

}
