package com.schemafy.core.erd.vendor.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByIdPort;
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
    GetDbVendorByIdPort {

  private final DbVendorRepository dbVendorRepository;
  private final DbVendorMapper dbVendorMapper;

  @Override
  public Flux<DbVendorSummary> findAllSummaries() {
    return dbVendorRepository.findAllActive()
        .map(dbVendorMapper::toSummary);
  }

  @Override
  public Mono<DbVendor> findActiveById(Integer id) {
    return dbVendorRepository.findActiveById(id)
        .map(dbVendorMapper::toDomain);
  }

}
