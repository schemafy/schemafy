package com.schemafy.core.erd.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.request.ChangeSchemaNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateSchemaRequest;
import com.schemafy.core.erd.controller.dto.response.SchemaResponse;
import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemaUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class SchemaController {

  private final CreateSchemaUseCase createSchemaUseCase;
  private final GetSchemaUseCase getSchemaUseCase;
  private final ChangeSchemaNameUseCase changeSchemaNameUseCase;
  private final DeleteSchemaUseCase deleteSchemaUseCase;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/schemas")
  public Mono<BaseResponse<SchemaResponse>> createSchema(
      @Valid @RequestBody CreateSchemaRequest request) {
    CreateSchemaCommand command = new CreateSchemaCommand(
        request.projectId(),
        request.dbVendorName(),
        request.name(),
        request.charset(),
        request.collation());
    return createSchemaUseCase.createSchema(command)
        .map(SchemaResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}")
  public Mono<BaseResponse<SchemaResponse>> getSchema(
      @PathVariable String schemaId) {
    GetSchemaQuery query = new GetSchemaQuery(schemaId);
    return getSchemaUseCase.getSchema(query)
        .map(SchemaResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/schemas/{schemaId}/name")
  public Mono<BaseResponse<Void>> changeSchemaName(
      @PathVariable String schemaId,
      @Valid @RequestBody ChangeSchemaNameRequest request) {
    ChangeSchemaNameCommand command = new ChangeSchemaNameCommand(
        request.projectId(),
        schemaId,
        request.newName());
    return changeSchemaNameUseCase.changeSchemaName(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/schemas/{schemaId}")
  public Mono<BaseResponse<Void>> deleteSchema(
      @PathVariable String schemaId) {
    DeleteSchemaCommand command = new DeleteSchemaCommand(schemaId);
    return deleteSchemaUseCase.deleteSchema(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

}
