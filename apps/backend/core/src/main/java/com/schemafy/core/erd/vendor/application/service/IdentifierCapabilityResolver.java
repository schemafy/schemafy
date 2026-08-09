package com.schemafy.core.erd.vendor.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.VendorCapabilities;
import com.schemafy.core.project.application.access.ProjectAccessResourceType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class IdentifierCapabilityResolver {

  private final ProjectDbVendorResolver projectDbVendorResolver;

  public Mono<IdentifierCapabilities> resolve(
      ProjectAccessResourceType resourceType,
      String resourceId) {
    return projectDbVendorResolver.resolve(resourceType, resourceId)
        .map(DbVendor::capabilities)
        .map(VendorCapabilities::identifiers);
  }

}
