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
import com.schemafy.core.erd.repository.entity.Column;
import com.schemafy.core.erd.service.ColumnService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation.ChangeColumnNameRequest;
import validation.Validation.ChangeColumnPositionRequest;
import validation.Validation.ChangeColumnTypeRequest;
import validation.Validation.CreateColumnRequest;
import validation.Validation.DeleteColumnRequest;

@RestController
@AllArgsConstructor
@RequestMapping(ApiPath.AUTH_API)
public class ColumnController {

    private final ColumnService columnService;

    @PostMapping("/columns")
    public Mono<BaseResponse<AffectedMappingResponse>> createColumn(
            @RequestBody CreateColumnRequest request) {
        return columnService.createColumn(request)
                .map(BaseResponse::success);
    }

    @GetMapping("/columns/{columnId}")
    public Mono<BaseResponse<Column>> getColumn(
            @PathVariable String columnId) {
        return columnService.getColumn(columnId)
                .map(BaseResponse::success);
    }

    @GetMapping("/columns/table/{tableId}")
    public Mono<BaseResponse<List<Column>>> getColumnsByTableId(
            @PathVariable String tableId) {
        return columnService.getColumnsByTableId(tableId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PutMapping("/columns/{columnId}/name")
    public Mono<BaseResponse<Column>> updateColumnName(
            @PathVariable String columnId,
            @RequestBody ChangeColumnNameRequest request) {
        return columnService.updateColumnName(request)
                .map(BaseResponse::success);
    }

    @PutMapping("/columns/{columnId}/type")
    public Mono<BaseResponse<Column>> updateColumnType(
            @PathVariable String columnId,
            @RequestBody ChangeColumnTypeRequest request) {
        return columnService.updateColumnType(request)
                .map(BaseResponse::success);
    }

    @PutMapping("/columns/{columnId}/position")
    public Mono<BaseResponse<Column>> updateColumnPosition(
            @PathVariable String columnId,
            @RequestBody ChangeColumnPositionRequest request) {
        return columnService.updateColumnPosition(request)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/columns/{columnId}")
    public Mono<BaseResponse<Void>> deleteColumn(
            @PathVariable String columnId,
            @RequestBody DeleteColumnRequest request) {
        return columnService.deleteColumn(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
