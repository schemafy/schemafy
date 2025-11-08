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
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.service.ConstraintService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation.AddColumnToConstraintRequest;
import validation.Validation.ChangeConstraintNameRequest;
import validation.Validation.CreateConstraintRequest;
import validation.Validation.DeleteConstraintRequest;
import validation.Validation.RemoveColumnFromConstraintRequest;

@RestController
@AllArgsConstructor
@RequestMapping(ApiPath.AUTH_API)
public class ConstraintController {

    private final ConstraintService constraintService;

    @PostMapping("/constraints")
    public Mono<BaseResponse<AffectedMappingResponse>> createConstraint(
            @RequestBody CreateConstraintRequest request) {
        return constraintService.createConstraint(request)
                .map(BaseResponse::success);
    }

    @GetMapping("/constraints/{constraintId}")
    public Mono<BaseResponse<ConstraintResponse>> getConstraint(
            @PathVariable String constraintId) {
        return constraintService.getConstraint(constraintId)
                .map(BaseResponse::success);
    }

    @GetMapping("/constraints/table/{tableId}")
    public Mono<BaseResponse<List<ConstraintResponse>>> getConstraintsByTableId(
            @PathVariable String tableId) {
        return constraintService.getConstraintsByTableId(tableId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PutMapping("/constraints/{constraintId}/name")
    public Mono<BaseResponse<ConstraintResponse>> updateConstraintName(
            @PathVariable String constraintId,
            @RequestBody ChangeConstraintNameRequest request) {
        if (!constraintId.equals(request.getConstraintId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return constraintService.updateConstraintName(request)
                .map(BaseResponse::success);
    }

    @PostMapping("/constraints/{constraintId}/columns")
    public Mono<BaseResponse<ConstraintColumnResponse>> addColumnToConstraint(
            @PathVariable String constraintId,
            @RequestBody AddColumnToConstraintRequest request) {
        if (!constraintId.equals(request.getConstraintId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return constraintService.addColumnToConstraint(request)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/constraints/{constraintId}/columns/{columnId}")
    public Mono<BaseResponse<Void>> removeColumnFromConstraint(
            @PathVariable String constraintId,
            @PathVariable String columnId,
            @RequestBody RemoveColumnFromConstraintRequest request) {
        if (!constraintId.equals(request.getConstraintId())
                || !columnId.equals(request.getConstraintColumnId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return constraintService.removeColumnFromConstraint(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

    @DeleteMapping("/constraints/{constraintId}")
    public Mono<BaseResponse<Void>> deleteConstraint(
            @PathVariable String constraintId,
            @RequestBody DeleteConstraintRequest request) {
        if (!constraintId.equals(request.getConstraintId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return constraintService.deleteConstraint(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
