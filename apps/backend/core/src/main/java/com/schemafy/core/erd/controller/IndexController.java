package com.schemafy.core.erd.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.request.UpdateIndexColumnSortDirRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateIndexTypeRequest;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.service.IndexService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation.AddColumnToIndexRequest;
import validation.Validation.ChangeIndexNameRequest;
import validation.Validation.CreateIndexRequest;
import validation.Validation.DeleteIndexRequest;
import validation.Validation.RemoveColumnFromIndexRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.API)
public class IndexController {

  private final IndexService indexService;

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PostMapping("/indexes")
  public Mono<BaseResponse<AffectedMappingResponse>> createIndex(
      @RequestBody CreateIndexRequest request) {
    return indexService.createIndex(request)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/indexes/{indexId}")
  public Mono<BaseResponse<IndexResponse>> getIndex(
      @PathVariable String indexId) {
    return indexService.getIndex(indexId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PutMapping("/indexes/{indexId}/name")
  public Mono<BaseResponse<IndexResponse>> updateIndexName(
      @PathVariable String indexId,
      @RequestBody ChangeIndexNameRequest request) {
    if (!indexId.equals(request.getIndexId())) {
      return Mono.error(
          new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
    }
    return indexService.updateIndexName(request)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PutMapping("/indexes/{indexId}/type")
  public Mono<BaseResponse<IndexResponse>> updateIndexType(
      @PathVariable String indexId,
      @RequestBody UpdateIndexTypeRequest request) {
    return indexService.updateIndexType(indexId, request)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PostMapping("/indexes/{indexId}/columns")
  public Mono<BaseResponse<IndexColumnResponse>> addColumnToIndex(
      @PathVariable String indexId,
      @RequestBody AddColumnToIndexRequest request) {
    if (!indexId.equals(request.getIndexId())) {
      return Mono.error(
          new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
    }
    return indexService.addColumnToIndex(request)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PutMapping("/indexes/{indexId}/columns/{indexColumnId}/sort-dir")
  public Mono<BaseResponse<IndexColumnResponse>> updateIndexColumnSortDir(
      @PathVariable String indexId,
      @PathVariable String indexColumnId,
      @RequestBody UpdateIndexColumnSortDirRequest request) {
    return indexService
        .updateIndexColumnSortDir(indexId, indexColumnId, request)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @DeleteMapping("/indexes/{indexId}/columns/{columnId}")
  public Mono<BaseResponse<Void>> removeColumnFromIndex(
      @PathVariable String indexId,
      @PathVariable String columnId,
      @RequestBody RemoveColumnFromIndexRequest request) {
    if (!indexId.equals(request.getIndexId())
        || !columnId.equals(request.getIndexColumnId())) {
      return Mono.error(
          new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
    }
    return indexService.removeColumnFromIndex(request)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @DeleteMapping("/indexes/{indexId}")
  public Mono<BaseResponse<Void>> deleteIndex(
      @PathVariable String indexId,
      @RequestBody DeleteIndexRequest request) {
    if (!indexId.equals(request.getIndexId())) {
      return Mono.error(
          new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
    }
    return indexService.deleteIndex(request)
        .then(Mono.just(BaseResponse.success(null)));
  }

}
