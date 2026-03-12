package com.schemafy.core.erd.vendor.application.port.in;

import com.schemafy.core.erd.vendor.domain.DbVendorSummary;

import reactor.core.publisher.Flux;

public interface ListDbVendorsUseCase {

  Flux<DbVendorSummary> listDbVendors();

}
