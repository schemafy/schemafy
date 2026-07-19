package com.schemafy.core.erd.vendor.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetDbVendorUseCase;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByIdPort;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetDbVendorService implements GetDbVendorUseCase {

  private final GetDbVendorByIdPort getDbVendorByIdPort;

  @Override
  public Mono<DbVendor> getDbVendor(GetDbVendorQuery query) {
    return getDbVendorByIdPort.findActiveById(query.id())
        .switchIfEmpty(Mono.error(
            new DomainException(VendorErrorCode.NOT_FOUND, "DB Vendor not found: " + query.id())));
  }

}
