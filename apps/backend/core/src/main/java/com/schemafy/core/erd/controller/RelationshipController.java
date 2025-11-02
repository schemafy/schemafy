package com.schemafy.core.erd.controller;

import java.util.List;

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
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;
import com.schemafy.core.erd.service.RelationshipService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import validation.Validation;

@RestController
@AllArgsConstructor
@RequestMapping(ApiPath.AUTH_API)
public class RelationshipController {

    private final RelationshipService relationshipService;

    @PostMapping("/relationships")
    public Mono<BaseResponse<AffectedMappingResponse>> createRelationship(
            @RequestBody Validation.CreateRelationshipRequest request,
            @RequestParam(required = false) String extra) {
        CreateRelationshipRequestWithExtra requestWithExtra = new CreateRelationshipRequestWithExtra(request, extra);
        return relationshipService.createRelationship(requestWithExtra)
                .map(BaseResponse::success);
    }

    @GetMapping("/relationships/{relationshipId}")
    public Mono<BaseResponse<Relationship>> getRelationship(
            @PathVariable String relationshipId) {
        return relationshipService.getRelationship(relationshipId)
                .map(BaseResponse::success);
    }

    @GetMapping("/relationships/table/{tableId}")
    public Mono<BaseResponse<List<Relationship>>> getRelationshipsByTableId(
            @PathVariable String tableId) {
        return relationshipService.getRelationshipsByTableId(tableId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PutMapping("/relationships/{relationshipId}/name")
    public Mono<BaseResponse<Relationship>> updateRelationshipName(
            @PathVariable String relationshipId,
            @RequestBody Validation.ChangeRelationshipNameRequest request) {
        return relationshipService.updateRelationshipName(request)
                .map(BaseResponse::success);
    }

    @PutMapping("/relationships/{relationshipId}/cardinality")
    public Mono<BaseResponse<Relationship>> updateRelationshipCardinality(
            @PathVariable String relationshipId,
            @RequestBody Validation.ChangeRelationshipCardinalityRequest request) {
        return relationshipService.updateRelationshipCardinality(request)
                .map(BaseResponse::success);
    }

    @PostMapping("/relationships/{relationshipId}/columns")
    public Mono<BaseResponse<RelationshipColumn>> addColumnToRelationship(
            @PathVariable String relationshipId,
            @RequestBody Validation.AddColumnToRelationshipRequest request) {
        return relationshipService.addColumnToRelationship(request)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/relationships/{relationshipId}/columns/{columnId}")
    public Mono<BaseResponse<Void>> removeColumnFromRelationship(
            @PathVariable String relationshipId,
            @PathVariable String columnId,
            @RequestBody Validation.RemoveColumnFromRelationshipRequest request) {
        return relationshipService.removeColumnFromRelationship(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

    @DeleteMapping("/relationships/{relationshipId}")
    public Mono<BaseResponse<Void>> deleteRelationship(
            @PathVariable String relationshipId,
            @RequestBody Validation.DeleteRelationshipRequest request) {
        return relationshipService.deleteRelationship(request)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
