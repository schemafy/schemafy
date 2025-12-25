package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.repository.DbVendorRepository;
import com.schemafy.core.erd.repository.entity.DbVendor;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DbVendorService {

  private final DbVendorRepository dbVendorRepository;

  public Mono<DbVendor> getVendorByDisplayName(String displayName) {
    return dbVendorRepository
        .findByIdAndDeletedAtIsNull(displayName)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.ERD_VENDOR_NOT_FOUND)));
  }

  public Flux<DbVendor> getAllVendors() { return dbVendorRepository.findByDeletedAtIsNull(); }

  public Mono<DbVendor> getVendorByNameAndVersion(String name,
      String version) {
    return dbVendorRepository.findByNameAndVersionAndDeletedAtIsNull(name,
        version)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.ERD_VENDOR_NOT_FOUND)));
  }

}
