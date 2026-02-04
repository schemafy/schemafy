package com.schemafy.core.erd.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.response.ExportResponse;
import com.schemafy.core.erd.service.ExportService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.API)
public class ExportController {

  private final ExportService exportService;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}/export/mysql-ddl")
  public Mono<BaseResponse<ExportResponse>> exportSchemaToMySqlDdl(
      @PathVariable String schemaId) {
    return exportService.exportSchemaToMySqlDdl(schemaId)
        .map(BaseResponse::success);
  }

}
