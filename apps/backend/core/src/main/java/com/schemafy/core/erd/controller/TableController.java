package com.schemafy.core.erd.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.entity.Table;
import com.schemafy.core.erd.service.TableService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation.ChangeTableNameRequest;
import validation.Validation.DeleteTableRequest;

@RestController
@AllArgsConstructor
public class TableController {

    private final TableService tableService;

    @PostMapping("/tables")
    public Mono<BaseResponse<AffectedMappingResponse>> createTable(
            @RequestBody CreateTableRequestWithExtra request) {
        return tableService.createTable(request)           
                .map(BaseResponse::success);
    }

    @GetMapping("/tables/{tableId}")
    public Mono<BaseResponse<Table>> getTable(
            @PathVariable String tableId) {
        return tableService.getTable(tableId)
                .map(BaseResponse::success);
    }

    @PutMapping("/tables/{tableId}/name")
    public Mono<BaseResponse<Table>> updateTableName(
            @PathVariable String tableId,
            @RequestBody ChangeTableNameRequest request) {
        if (request.getTableId() != null
                && !tableId.equals(request.getTableId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return tableService.updateTableName(request)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/tables/{tableId}")
    public Mono<BaseResponse<Void>> deleteTable(
            @PathVariable String tableId,
            @RequestBody DeleteTableRequest request) {
        if (request.getTableId() != null
                && !tableId.equals(request.getTableId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return tableService.deleteTable(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
