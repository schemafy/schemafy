package com.schemafy.core.erd.vendor.application.port.out;

import com.schemafy.core.erd.vendor.domain.DbVendorSummary;

import reactor.core.publisher.Flux;

public interface ListDbVendorSummariesPort {

  Flux<DbVendorSummary> findAllSummaries();

}
