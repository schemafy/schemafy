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
import com.schemafy.core.erd.controller.dto.request.AddIndexColumnRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexColumnSortDirectionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexNameRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexTypeRequest;
import com.schemafy.core.erd.controller.dto.request.CreateIndexColumnRequest;
import com.schemafy.core.erd.controller.dto.request.CreateIndexRequest;
import com.schemafy.core.erd.controller.dto.response.AddIndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class IndexController {

  private final CreateIndexUseCase createIndexUseCase;
  private final GetIndexUseCase getIndexUseCase;
  private final GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;
  private final ChangeIndexNameUseCase changeIndexNameUseCase;
  private final ChangeIndexTypeUseCase changeIndexTypeUseCase;
  private final DeleteIndexUseCase deleteIndexUseCase;
  private final GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;
  private final AddIndexColumnUseCase addIndexColumnUseCase;
  private final RemoveIndexColumnUseCase removeIndexColumnUseCase;
  private final GetIndexColumnUseCase getIndexColumnUseCase;
  private final ChangeIndexColumnPositionUseCase changeIndexColumnPositionUseCase;
  private final ChangeIndexColumnSortDirectionUseCase changeIndexColumnSortDirectionUseCase;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/indexes")
  public Mono<BaseResponse<MutationResponse<IndexResponse>>> createIndex(
      @Valid @RequestBody CreateIndexRequest request) {
    CreateIndexCommand command = new CreateIndexCommand(
        request.tableId(),
        request.name(),
        request.type(),
        mapIndexColumns(request.columns()));
    return createIndexUseCase.createIndex(command)
        .map(result -> MutationResponse.of(
            IndexResponse.from(result.result(), request.tableId()),
            result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/indexes/{indexId}")
  public Mono<BaseResponse<IndexResponse>> getIndex(
      @PathVariable String indexId) {
    GetIndexQuery query = new GetIndexQuery(indexId);
    return getIndexUseCase.getIndex(query)
        .map(IndexResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}/indexes")
  public Mono<BaseResponse<List<IndexResponse>>> getIndexesByTableId(
      @PathVariable String tableId) {
    GetIndexesByTableIdQuery query = new GetIndexesByTableIdQuery(tableId);
    return getIndexesByTableIdUseCase.getIndexesByTableId(query)
        .map(indexes -> indexes.stream()
            .map(IndexResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/indexes/{indexId}/name")
  public Mono<BaseResponse<MutationResponse<Void>>> changeIndexName(
      @PathVariable String indexId,
      @Valid @RequestBody ChangeIndexNameRequest request) {
    ChangeIndexNameCommand command = new ChangeIndexNameCommand(
        indexId,
        request.newName());
    return changeIndexNameUseCase.changeIndexName(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/indexes/{indexId}/type")
  public Mono<BaseResponse<MutationResponse<Void>>> changeIndexType(
      @PathVariable String indexId,
      @Valid @RequestBody ChangeIndexTypeRequest request) {
    ChangeIndexTypeCommand command = new ChangeIndexTypeCommand(
        indexId,
        request.type());
    return changeIndexTypeUseCase.changeIndexType(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/indexes/{indexId}")
  public Mono<BaseResponse<MutationResponse<Void>>> deleteIndex(
      @PathVariable String indexId) {
    DeleteIndexCommand command = new DeleteIndexCommand(indexId);
    return deleteIndexUseCase.deleteIndex(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/indexes/{indexId}/columns")
  public Mono<BaseResponse<List<IndexColumnResponse>>> getIndexColumns(
      @PathVariable String indexId) {
    GetIndexColumnsByIndexIdQuery query = new GetIndexColumnsByIndexIdQuery(indexId);
    return getIndexColumnsByIndexIdUseCase.getIndexColumnsByIndexId(query)
        .map(columns -> columns.stream()
            .map(IndexColumnResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/indexes/{indexId}/columns")
  public Mono<BaseResponse<MutationResponse<AddIndexColumnResponse>>> addIndexColumn(
      @PathVariable String indexId,
      @Valid @RequestBody AddIndexColumnRequest request) {
    AddIndexColumnCommand command = new AddIndexColumnCommand(
        indexId,
        request.columnId(),
        request.seqNo(),
        request.sortDirection());
    return addIndexColumnUseCase.addIndexColumn(command)
        .map(result -> MutationResponse.of(
            AddIndexColumnResponse.from(result.result()),
            result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @DeleteMapping("/indexes/{indexId}/columns/{indexColumnId}")
  public Mono<BaseResponse<MutationResponse<Void>>> removeIndexColumn(
      @PathVariable String indexId,
      @PathVariable String indexColumnId) {
    RemoveIndexColumnCommand command = new RemoveIndexColumnCommand(indexId, indexColumnId);
    return removeIndexColumnUseCase.removeIndexColumn(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/index-columns/{indexColumnId}")
  public Mono<BaseResponse<IndexColumnResponse>> getIndexColumn(
      @PathVariable String indexColumnId) {
    GetIndexColumnQuery query = new GetIndexColumnQuery(indexColumnId);
    return getIndexColumnUseCase.getIndexColumn(query)
        .map(IndexColumnResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/index-columns/{indexColumnId}/position")
  public Mono<BaseResponse<MutationResponse<Void>>> changeIndexColumnPosition(
      @PathVariable String indexColumnId,
      @RequestBody ChangeIndexColumnPositionRequest request) {
    ChangeIndexColumnPositionCommand command = new ChangeIndexColumnPositionCommand(
        indexColumnId,
        request.seqNo());
    return changeIndexColumnPositionUseCase.changeIndexColumnPosition(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/index-columns/{indexColumnId}/sort-direction")
  public Mono<BaseResponse<MutationResponse<Void>>> changeIndexColumnSortDirection(
      @PathVariable String indexColumnId,
      @Valid @RequestBody ChangeIndexColumnSortDirectionRequest request) {
    ChangeIndexColumnSortDirectionCommand command = new ChangeIndexColumnSortDirectionCommand(
        indexColumnId,
        request.sortDirection());
    return changeIndexColumnSortDirectionUseCase.changeIndexColumnSortDirection(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  private static List<CreateIndexColumnCommand> mapIndexColumns(
      List<CreateIndexColumnRequest> columns) {
    if (columns == null || columns.isEmpty()) {
      return List.of();
    }
    return columns.stream()
        .map(column -> new CreateIndexColumnCommand(
            column.columnId(),
            column.seqNo(),
            column.sortDirection()))
        .toList();
  }

}
