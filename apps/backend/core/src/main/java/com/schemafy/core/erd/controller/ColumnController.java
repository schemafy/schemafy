package com.schemafy.core.erd.controller;

import java.util.List;

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
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
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
@RequestMapping(ApiPath.API)
public class ColumnController {

    private final ColumnService columnService;

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PostMapping("/columns")
    public Mono<BaseResponse<AffectedMappingResponse>> createColumn(
            @RequestBody CreateColumnRequest request) {
        return columnService.createColumn(request)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/columns/{columnId}")
    public Mono<BaseResponse<ColumnResponse>> getColumn(
            @PathVariable String columnId) {
        return columnService.getColumn(columnId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/columns/table/{tableId}")
    public Mono<BaseResponse<List<ColumnResponse>>> getColumnsByTableId(
            @PathVariable String tableId) {
        return columnService.getColumnsByTableId(tableId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PutMapping("/columns/{columnId}/name")
    public Mono<BaseResponse<ColumnResponse>> updateColumnName(
            @PathVariable String columnId,
            @RequestBody ChangeColumnNameRequest request) {
        if (!columnId.equals(request.getColumnId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return columnService.updateColumnName(request)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PutMapping("/columns/{columnId}/type")
    public Mono<BaseResponse<ColumnResponse>> updateColumnType(
            @PathVariable String columnId,
            @RequestBody ChangeColumnTypeRequest request) {
        if (!columnId.equals(request.getColumnId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return columnService.updateColumnType(request)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PutMapping("/columns/{columnId}/position")
    public Mono<BaseResponse<ColumnResponse>> updateColumnPosition(
            @PathVariable String columnId,
            @RequestBody ChangeColumnPositionRequest request) {
        if (!columnId.equals(request.getColumnId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return columnService.updateColumnPosition(request)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @DeleteMapping("/columns/{columnId}")
    public Mono<BaseResponse<Void>> deleteColumn(
            @PathVariable String columnId,
            @RequestBody DeleteColumnRequest request) {
        if (!columnId.equals(request.getColumnId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return columnService.deleteColumn(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
