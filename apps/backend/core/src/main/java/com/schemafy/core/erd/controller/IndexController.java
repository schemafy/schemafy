package com.schemafy.core.erd.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;
import com.schemafy.core.erd.service.IndexService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation.AddColumnToIndexRequest;
import validation.Validation.ChangeIndexNameRequest;
import validation.Validation.CreateIndexRequest;
import validation.Validation.DeleteIndexRequest;
import validation.Validation.RemoveColumnFromIndexRequest;

@RestController
@AllArgsConstructor
@RequestMapping(ApiPath.AUTH_API)
public class IndexController {

    private final IndexService indexService;

    @PostMapping("/indexes")
    public Mono<BaseResponse<AffectedMappingResponse>> createIndex(
            @RequestBody CreateIndexRequest request) {
        return indexService.createIndex(request)
                .map(BaseResponse::success);
    }

    @GetMapping("/indexes/{indexId}")
    public Mono<BaseResponse<Index>> getIndex(
            @PathVariable String indexId) {
        return indexService.getIndex(indexId)
                .map(BaseResponse::success);
    }

    @GetMapping("/indexes/table/{tableId}")
    public Mono<BaseResponse<List<Index>>> getIndexesByTableId(
            @PathVariable String tableId) {
        return indexService.getIndexesByTableId(tableId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PutMapping("/indexes/{indexId}/name")
    public Mono<BaseResponse<Index>> updateIndexName(
            @PathVariable String indexId,
            @RequestBody ChangeIndexNameRequest request) {
        return indexService.updateIndexName(request)
                .map(BaseResponse::success);
    }

    @PostMapping("/indexes/{indexId}/columns")
    public Mono<BaseResponse<IndexColumn>> addColumnToIndex(
            @PathVariable String indexId,
            @RequestBody AddColumnToIndexRequest request) {
        return indexService.addColumnToIndex(request)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/indexes/{indexId}/columns/{columnId}")
    public Mono<BaseResponse<Void>> removeColumnFromIndex(
            @PathVariable String indexId,
            @PathVariable String columnId,
            @RequestBody RemoveColumnFromIndexRequest request) {
        return indexService.removeColumnFromIndex(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

    @DeleteMapping("/indexes/{indexId}")
    public Mono<BaseResponse<Void>> deleteIndex(
            @PathVariable String indexId,
            @RequestBody DeleteIndexRequest request) {
        return indexService.deleteIndex(request)
                .then(Mono.just(BaseResponse.success(null)));
    }
}
