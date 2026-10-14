package com.schemafy.core.erd.vendor.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.project.application.access.GetProjectIdByAccessResourcePort;
import com.schemafy.core.project.application.access.ProjectAccessResourceType;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProjectDbVendorResolver {

  private final GetProjectIdByAccessResourcePort getProjectIdByAccessResourcePort;
  private final GetProjectDbVendorUseCase getProjectDbVendorUseCase;

  public Mono<DbVendor> resolve(
      ProjectAccessResourceType resourceType,
      String resourceId) {
    return getProjectIdByAccessResourcePort.findProjectId(resourceType, resourceId)
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.NOT_FOUND,
            "Project not found for %s: %s".formatted(resourceType, resourceId))))
        .flatMap(projectId -> getProjectDbVendorUseCase.getProjectDbVendor(
            new GetProjectDbVendorQuery(projectId)));
  }

}
