package com.schemafy.core.erd.vendor.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;
import com.schemafy.core.project.application.access.ProjectAccessResourceType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DatatypePolicyResolver {

  private final ProjectDbVendorResolver projectDbVendorResolver;

  public Mono<DatatypePolicy> resolve(
      ProjectAccessResourceType resourceType,
      String resourceId) {
    return projectDbVendorResolver.resolve(resourceType, resourceId)
        .map(DbVendor::datatypeMappings);
  }

}
