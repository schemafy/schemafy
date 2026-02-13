package com.schemafy.domain.erd.vendor.application.port.out;

import com.schemafy.domain.erd.vendor.domain.DbVendorSummary;

import reactor.core.publisher.Flux;

public interface ListDbVendorSummariesPort {

  Flux<DbVendorSummary> findAllSummaries();

}
