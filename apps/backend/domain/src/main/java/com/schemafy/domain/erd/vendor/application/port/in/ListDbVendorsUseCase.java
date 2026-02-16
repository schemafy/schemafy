package com.schemafy.domain.erd.vendor.application.port.in;

import com.schemafy.domain.erd.vendor.domain.DbVendorSummary;

import reactor.core.publisher.Flux;

public interface ListDbVendorsUseCase {

  Flux<DbVendorSummary> listDbVendors();

}
