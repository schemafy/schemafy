package com.schemafy.core.erd.vendor.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByDisplayNamePort;
import com.schemafy.core.erd.vendor.application.port.out.ListDbVendorSummariesPort;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.DbVendorSummary;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class DbVendorPersistenceAdapter implements
    ListDbVendorSummariesPort,
    GetDbVendorByDisplayNamePort {

  private final DbVendorRepository dbVendorRepository;
  private final DbVendorMapper dbVendorMapper;

  @Override
  public Flux<DbVendorSummary> findAllSummaries() {
    return dbVendorRepository.findAll()
        .map(dbVendorMapper::toSummary);
  }

  @Override
  public Mono<DbVendor> findByDisplayName(String displayName) {
    return dbVendorRepository.findById(displayName)
        .map(dbVendorMapper::toDomain);
  }

}
