package com.schemafy.domain.erd.vendor.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.vendor.application.port.in.ListDbVendorsUseCase;
import com.schemafy.domain.erd.vendor.application.port.out.ListDbVendorSummariesPort;
import com.schemafy.domain.erd.vendor.domain.DbVendorSummary;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
class ListDbVendorsService implements ListDbVendorsUseCase {

  private final ListDbVendorSummariesPort listDbVendorSummariesPort;

  @Override
  public Flux<DbVendorSummary> listDbVendors() {
    return listDbVendorSummariesPort.findAllSummaries();
  }

}
