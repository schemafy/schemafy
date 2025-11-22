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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.service.TableService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import validation.Validation.ChangeTableNameRequest;
import validation.Validation.CreateTableRequest;
import validation.Validation.DeleteTableRequest;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(ApiPath.API)
public class TableController {

    private final TableService tableService;

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PostMapping("/tables")
    public Mono<BaseResponse<AffectedMappingResponse>> createTable(
            @RequestBody CreateTableRequest request,
            @RequestParam(required = false, defaultValue = "{}") String extra) {
        CreateTableRequestWithExtra requestWithExtra = new CreateTableRequestWithExtra(
                request, extra);

        return tableService.createTable(requestWithExtra)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/tables/{tableId}")
    public Mono<BaseResponse<TableDetailResponse>> getTable(
            @PathVariable String tableId) {
        return tableService.getTable(tableId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/tables/schema/{schemaId}")
    public Mono<BaseResponse<List<TableResponse>>> getTablesBySchemaId(
            @PathVariable String schemaId) {
        return tableService.getTablesBySchemaId(schemaId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PutMapping("/tables/{tableId}/name")
    public Mono<BaseResponse<TableResponse>> updateTableName(
            @PathVariable String tableId,
            @RequestBody ChangeTableNameRequest request) {
        if (!tableId.equals(request.getTableId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return tableService.updateTableName(request)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @DeleteMapping("/tables/{tableId}")
    public Mono<BaseResponse<Void>> deleteTable(
            @PathVariable String tableId,
            @RequestBody DeleteTableRequest request) {
        if (!tableId.equals(request.getTableId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return tableService.deleteTable(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
