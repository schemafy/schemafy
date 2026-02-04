package com.schemafy.core.erd.controller;

import java.util.List;

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
import com.schemafy.core.common.type.MutationResponse;
import com.schemafy.core.erd.controller.dto.request.AddRelationshipColumnRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipCardinalityRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipExtraRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipKindRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequest;
import com.schemafy.core.erd.controller.dto.response.AddRelationshipColumnResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipExtraUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class RelationshipController {

  private final CreateRelationshipUseCase createRelationshipUseCase;
  private final GetRelationshipUseCase getRelationshipUseCase;
  private final GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;
  private final ChangeRelationshipNameUseCase changeRelationshipNameUseCase;
  private final ChangeRelationshipKindUseCase changeRelationshipKindUseCase;
  private final ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase;
  private final ChangeRelationshipExtraUseCase changeRelationshipExtraUseCase;
  private final DeleteRelationshipUseCase deleteRelationshipUseCase;
  private final GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;
  private final AddRelationshipColumnUseCase addRelationshipColumnUseCase;
  private final RemoveRelationshipColumnUseCase removeRelationshipColumnUseCase;
  private final GetRelationshipColumnUseCase getRelationshipColumnUseCase;
  private final ChangeRelationshipColumnPositionUseCase changeRelationshipColumnPositionUseCase;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/relationships")
  public Mono<BaseResponse<MutationResponse<RelationshipResponse>>> createRelationship(
      @Valid @RequestBody CreateRelationshipRequest request) {
    CreateRelationshipCommand command = new CreateRelationshipCommand(
        request.fkTableId(),
        request.pkTableId(),
        request.kind(),
        request.cardinality());
    return createRelationshipUseCase.createRelationship(command)
        .map(result -> MutationResponse.of(
            RelationshipResponse.from(result.result()),
            result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/relationships/{relationshipId}")
  public Mono<BaseResponse<RelationshipResponse>> getRelationship(
      @PathVariable String relationshipId) {
    GetRelationshipQuery query = new GetRelationshipQuery(relationshipId);
    return getRelationshipUseCase.getRelationship(query)
        .map(RelationshipResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}/relationships")
  public Mono<BaseResponse<List<RelationshipResponse>>> getRelationshipsByTableId(
      @PathVariable String tableId) {
    GetRelationshipsByTableIdQuery query = new GetRelationshipsByTableIdQuery(tableId);
    return getRelationshipsByTableIdUseCase.getRelationshipsByTableId(query)
        .map(relationships -> relationships.stream()
            .map(RelationshipResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/relationships/{relationshipId}/name")
  public Mono<BaseResponse<MutationResponse<Void>>> changeRelationshipName(
      @PathVariable String relationshipId,
      @Valid @RequestBody ChangeRelationshipNameRequest request) {
    ChangeRelationshipNameCommand command = new ChangeRelationshipNameCommand(
        relationshipId,
        request.newName());
    return changeRelationshipNameUseCase.changeRelationshipName(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/relationships/{relationshipId}/kind")
  public Mono<BaseResponse<MutationResponse<Void>>> changeRelationshipKind(
      @PathVariable String relationshipId,
      @Valid @RequestBody ChangeRelationshipKindRequest request) {
    ChangeRelationshipKindCommand command = new ChangeRelationshipKindCommand(
        relationshipId,
        request.kind());
    return changeRelationshipKindUseCase.changeRelationshipKind(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/relationships/{relationshipId}/cardinality")
  public Mono<BaseResponse<MutationResponse<Void>>> changeRelationshipCardinality(
      @PathVariable String relationshipId,
      @Valid @RequestBody ChangeRelationshipCardinalityRequest request) {
    ChangeRelationshipCardinalityCommand command = new ChangeRelationshipCardinalityCommand(
        relationshipId,
        request.cardinality());
    return changeRelationshipCardinalityUseCase.changeRelationshipCardinality(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/relationships/{relationshipId}/extra")
  public Mono<BaseResponse<MutationResponse<Void>>> changeRelationshipExtra(
      @PathVariable String relationshipId,
      @RequestBody ChangeRelationshipExtraRequest request) {
    ChangeRelationshipExtraCommand command = new ChangeRelationshipExtraCommand(
        relationshipId,
        request.extra());
    return changeRelationshipExtraUseCase.changeRelationshipExtra(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/relationships/{relationshipId}")
  public Mono<BaseResponse<MutationResponse<Void>>> deleteRelationship(
      @PathVariable String relationshipId) {
    DeleteRelationshipCommand command = new DeleteRelationshipCommand(relationshipId);
    return deleteRelationshipUseCase.deleteRelationship(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/relationships/{relationshipId}/columns")
  public Mono<BaseResponse<List<RelationshipColumnResponse>>> getRelationshipColumns(
      @PathVariable String relationshipId) {
    GetRelationshipColumnsByRelationshipIdQuery query =
        new GetRelationshipColumnsByRelationshipIdQuery(relationshipId);
    return getRelationshipColumnsByRelationshipIdUseCase
        .getRelationshipColumnsByRelationshipId(query)
        .map(columns -> columns.stream()
            .map(RelationshipColumnResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/relationships/{relationshipId}/columns")
  public Mono<BaseResponse<MutationResponse<AddRelationshipColumnResponse>>> addRelationshipColumn(
      @PathVariable String relationshipId,
      @Valid @RequestBody AddRelationshipColumnRequest request) {
    AddRelationshipColumnCommand command = new AddRelationshipColumnCommand(
        relationshipId,
        request.pkColumnId(),
        request.fkColumnId(),
        request.seqNo());
    return addRelationshipColumnUseCase.addRelationshipColumn(command)
        .map(result -> MutationResponse.of(
            AddRelationshipColumnResponse.from(result.result()),
            result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @DeleteMapping("/relationships/{relationshipId}/columns/{relationshipColumnId}")
  public Mono<BaseResponse<MutationResponse<Void>>> removeRelationshipColumn(
      @PathVariable String relationshipId,
      @PathVariable String relationshipColumnId) {
    RemoveRelationshipColumnCommand command = new RemoveRelationshipColumnCommand(
        relationshipId,
        relationshipColumnId);
    return removeRelationshipColumnUseCase.removeRelationshipColumn(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/relationship-columns/{relationshipColumnId}")
  public Mono<BaseResponse<RelationshipColumnResponse>> getRelationshipColumn(
      @PathVariable String relationshipColumnId) {
    GetRelationshipColumnQuery query = new GetRelationshipColumnQuery(relationshipColumnId);
    return getRelationshipColumnUseCase.getRelationshipColumn(query)
        .map(RelationshipColumnResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/relationship-columns/{relationshipColumnId}/position")
  public Mono<BaseResponse<MutationResponse<Void>>> changeRelationshipColumnPosition(
      @PathVariable String relationshipColumnId,
      @RequestBody ChangeRelationshipColumnPositionRequest request) {
    ChangeRelationshipColumnPositionCommand command = new ChangeRelationshipColumnPositionCommand(
        relationshipColumnId,
        request.seqNo());
    return changeRelationshipColumnPositionUseCase.changeRelationshipColumnPosition(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

}
