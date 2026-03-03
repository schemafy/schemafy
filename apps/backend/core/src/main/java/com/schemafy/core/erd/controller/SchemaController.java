package com.schemafy.core.erd.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.ObjectProvider;
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
import com.schemafy.core.common.type.MutationResponse;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster;
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
import com.schemafy.domain.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemasByProjectIdUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class SchemaController {

  private final CreateSchemaUseCase createSchemaUseCase;
  private final GetSchemaUseCase getSchemaUseCase;
  private final GetSchemasByProjectIdUseCase getSchemasByProjectIdUseCase;
  private final ChangeSchemaNameUseCase changeSchemaNameUseCase;
  private final DeleteSchemaUseCase deleteSchemaUseCase;

  private final ObjectProvider<ErdMutationBroadcaster> broadcasterProvider;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/schemas")
  public Mono<MutationResponse<SchemaResponse>> createSchema(
      @Valid @RequestBody CreateSchemaRequest request) {
    CreateSchemaCommand command = new CreateSchemaCommand(
        request.projectId(),
        request.dbVendorName(),
        request.name(),
        request.charset(),
        request.collation());
    return createSchemaUseCase.createSchema(command)
        .flatMap(result -> broadcastSchemaChange(
            result.result().id())
            .thenReturn(result))
        .map(result -> MutationResponse.of(
            SchemaResponse.from(result.result()),
            result.affectedTableIds()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}")
  public Mono<SchemaResponse> getSchema(
      @PathVariable String schemaId) {
    GetSchemaQuery query = new GetSchemaQuery(schemaId);
    return getSchemaUseCase.getSchema(query)
        .map(SchemaResponse::from);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/projects/{projectId}/schemas")
  public Mono<List<SchemaResponse>> getSchemasByProjectId(
      @PathVariable String projectId) {
    GetSchemasByProjectIdQuery query = new GetSchemasByProjectIdQuery(projectId);
    return getSchemasByProjectIdUseCase.getSchemasByProjectId(query)
        .map(SchemaResponse::from)
        .collectList();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/schemas/{schemaId}/name")
  public Mono<MutationResponse<Void>> changeSchemaName(
      @PathVariable String schemaId,
      @Valid @RequestBody ChangeSchemaNameRequest request) {
    ChangeSchemaNameCommand command = new ChangeSchemaNameCommand(
        schemaId,
        request.newName());
    return changeSchemaNameUseCase.changeSchemaName(command)
        .flatMap(result -> broadcastSchemaChange(schemaId)
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/schemas/{schemaId}")
  public Mono<MutationResponse<Void>> deleteSchema(
      @PathVariable String schemaId) {
    DeleteSchemaCommand command = new DeleteSchemaCommand(schemaId);
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return deleteSchemaUseCase.deleteSchema(command)
          .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
    }
    return broadcaster.resolveFromSchemaId(schemaId)
        .flatMap(ctx -> deleteSchemaUseCase.deleteSchema(command)
            .flatMap(result -> broadcaster
                .broadcastWithContext(ctx, result.affectedTableIds())
                .thenReturn(result)))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
  }

  private Mono<Void> broadcastSchemaChange(String schemaId) {
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return Mono.empty();
    }
    return broadcaster.broadcastSchemaChange(schemaId);
  }

}
