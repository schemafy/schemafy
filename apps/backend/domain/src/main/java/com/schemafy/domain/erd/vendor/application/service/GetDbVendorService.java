package com.schemafy.domain.erd.vendor.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorUseCase;
import com.schemafy.domain.erd.vendor.application.port.out.GetDbVendorByDisplayNamePort;
import com.schemafy.domain.erd.vendor.domain.DbVendor;
import com.schemafy.domain.erd.vendor.domain.exception.VendorErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetDbVendorService implements GetDbVendorUseCase {

  private final GetDbVendorByDisplayNamePort getDbVendorByDisplayNamePort;

  @Override
  public Mono<DbVendor> getDbVendor(GetDbVendorQuery query) {
    return getDbVendorByDisplayNamePort.findByDisplayName(query.displayName())
        .switchIfEmpty(Mono.error(
            new DomainException(VendorErrorCode.NOT_FOUND, "DB Vendor not found: " + query.displayName())));
  }

}
