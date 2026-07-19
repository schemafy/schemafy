package com.schemafy.core.erd.vendor.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.application.port.out.GetActiveProjectDbVendorIdPort;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByIdPort;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectDbVendorService implements GetProjectDbVendorUseCase {

  private final GetActiveProjectDbVendorIdPort getActiveProjectDbVendorIdPort;
  private final GetDbVendorByIdPort getDbVendorByIdPort;

  @Override
  public Mono<DbVendor> getProjectDbVendor(GetProjectDbVendorQuery query) {
    return getActiveProjectDbVendorIdPort.findDbVendorIdByProjectId(query.projectId())
        .switchIfEmpty(Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND)))
        .flatMap(dbVendorId -> getDbVendorByIdPort.findActiveById(dbVendorId)
            .switchIfEmpty(Mono.error(
                new DomainException(VendorErrorCode.NOT_FOUND,
                    "DB Vendor not found: " + dbVendorId))));
  }

}
