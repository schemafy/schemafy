package com.schemafy.core.erd.vendor.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByProjectIdPort;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectDbVendorService implements GetProjectDbVendorUseCase {

  private final GetDbVendorByProjectIdPort getDbVendorByProjectIdPort;

  @Override
  public Mono<DbVendor> getProjectDbVendor(GetProjectDbVendorQuery query) {
    return getDbVendorByProjectIdPort.findByProjectId(query.projectId())
        .switchIfEmpty(Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND)));
  }

}
