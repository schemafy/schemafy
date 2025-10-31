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
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.entity.Schema;
import com.schemafy.core.erd.service.SchemaService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation.ChangeSchemaNameRequest;
import validation.Validation.CreateSchemaRequest;
import validation.Validation.DeleteSchemaRequest;

@RestController
@AllArgsConstructor
public class SchemaController {

    private final SchemaService schemaService;

    @PostMapping("/schemas")
    public Mono<BaseResponse<AffectedMappingResponse>> createSchema(
            @RequestBody CreateSchemaRequest request) {
        return schemaService.createSchema(request)
                .map(BaseResponse::success);
    }

    @GetMapping("/schemas/{schemaId}")
    public Mono<BaseResponse<Schema>> getSchema(
            @PathVariable String schemaId) {
        return schemaService.getSchema(schemaId)
                .map(BaseResponse::success);
    }

    @PutMapping("/schemas/{schemaId}/name")
    public Mono<BaseResponse<Schema>> updateSchemaName(
            @PathVariable String schemaId,
            @RequestBody ChangeSchemaNameRequest request) {
        if (request.getSchemaId() != null
                && !schemaId.equals(request.getSchemaId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return schemaService
                .updateSchemaName(request)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/schemas/{schemaId}")
    public Mono<BaseResponse<Void>> deleteSchema(
            @PathVariable String schemaId,
            @RequestBody DeleteSchemaRequest request) {
        if (request.getSchemaId() != null
                && !schemaId.equals(request.getSchemaId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return schemaService.deleteSchema(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
