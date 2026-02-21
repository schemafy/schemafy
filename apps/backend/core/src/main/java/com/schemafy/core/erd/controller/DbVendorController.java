package com.schemafy.core.erd.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.erd.controller.dto.response.DbVendorDetailResponse;
import com.schemafy.core.erd.controller.dto.response.DbVendorSummaryResponse;
import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorUseCase;
import com.schemafy.domain.erd.vendor.application.port.in.ListDbVendorsUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class DbVendorController {

  private final ListDbVendorsUseCase listDbVendorsUseCase;
  private final GetDbVendorUseCase getDbVendorUseCase;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/vendors")
  public Mono<List<DbVendorSummaryResponse>> listVendors() {
    return listDbVendorsUseCase.listDbVendors()
        .map(DbVendorSummaryResponse::from)
        .collectList();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/vendors/{displayName}")
  public Mono<DbVendorDetailResponse> getVendor(
      @PathVariable String displayName) {
    GetDbVendorQuery query = new GetDbVendorQuery(displayName);
    return getDbVendorUseCase.getDbVendor(query)
        .map(DbVendorDetailResponse::from);
  }

}
