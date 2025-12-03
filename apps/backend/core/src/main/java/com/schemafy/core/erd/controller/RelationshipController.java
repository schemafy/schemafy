package com.schemafy.core.erd.controller;

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
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.service.RelationshipService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.API)
public class RelationshipController {

    private final RelationshipService relationshipService;

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PostMapping("/relationships")
    public Mono<BaseResponse<AffectedMappingResponse>> createRelationship(
            @RequestBody Validation.CreateRelationshipRequest request,
            @RequestParam(required = false, defaultValue = "{}") String extra) {
        CreateRelationshipRequestWithExtra requestWithExtra = new CreateRelationshipRequestWithExtra(
                request, extra);
        return relationshipService.createRelationship(requestWithExtra)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/relationships/{relationshipId}")
    public Mono<BaseResponse<RelationshipResponse>> getRelationship(
            @PathVariable String relationshipId) {
        return relationshipService.getRelationship(relationshipId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PutMapping("/relationships/{relationshipId}/name")
    public Mono<BaseResponse<RelationshipResponse>> updateRelationshipName(
            @PathVariable String relationshipId,
            @RequestBody Validation.ChangeRelationshipNameRequest request) {
        if (!relationshipId.equals(request.getRelationshipId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return relationshipService.updateRelationshipName(request)
                .map(BaseResponse::success);
    }

    @PutMapping("/relationships/{relationshipId}/cardinality")
    public Mono<BaseResponse<RelationshipResponse>> updateRelationshipCardinality(
            @PathVariable String relationshipId,
            @RequestBody Validation.ChangeRelationshipCardinalityRequest request) {
        if (!relationshipId.equals(request.getRelationshipId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return relationshipService.updateRelationshipCardinality(request)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @PostMapping("/relationships/{relationshipId}/columns")
    public Mono<BaseResponse<AffectedMappingResponse>> addColumnToRelationship(
            @PathVariable String relationshipId,
            @RequestBody Validation.AddColumnToRelationshipRequest request) {
        if (!relationshipId.equals(request.getRelationshipId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return relationshipService.addColumnToRelationship(request)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @DeleteMapping("/relationships/{relationshipId}/columns/{columnId}")
    public Mono<BaseResponse<Void>> removeColumnFromRelationship(
            @PathVariable String relationshipId,
            @PathVariable String columnId,
            @RequestBody Validation.RemoveColumnFromRelationshipRequest request) {
        if (!relationshipId.equals(request.getRelationshipId())
                || !columnId.equals(request.getRelationshipColumnId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return relationshipService.removeColumnFromRelationship(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @DeleteMapping("/relationships/{relationshipId}")
    public Mono<BaseResponse<Void>> deleteRelationship(
            @PathVariable String relationshipId,
            @RequestBody Validation.DeleteRelationshipRequest request) {
        if (!relationshipId.equals(request.getRelationshipId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return relationshipService.deleteRelationship(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
