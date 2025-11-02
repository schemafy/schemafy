package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@Service
@RequiredArgsConstructor
public class RelationshipService {

    private final ValidationClient validationClient;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipColumnRepository relationshipColumnRepository;
    private final AffectedEntitiesSaver affectedEntitiesSaver;

    public Mono<AffectedMappingResponse> createRelationship(
            CreateRelationshipRequestWithExtra request) {
        return validationClient.createRelationship(request.request())
                .flatMap(database -> relationshipRepository
                        .save(ErdMapper.toEntity(
                                request.request().getRelationship(),
                                request.extra()))
                        .flatMap(savedRelationship -> {
                            Validation.Database updatedDatabase = AffectedMappingResponse
                                    .updateEntityIdInDatabase(
                                            database,
                                            EntityType.RELATIONSHIP,
                                            request.request().getRelationship().getId(),
                                            savedRelationship.getId()
                                    );

                            return affectedEntitiesSaver
                                    .saveAffectedEntities(
                                            request.request().getDatabase(),
                                            updatedDatabase,
                                            savedRelationship.getId(),
                                            savedRelationship.getId(),
                                            "RELATIONSHIP"
                                    )
                                    .map(propagated -> AffectedMappingResponse.of(
                                            request.request(),
                                            request.request().getDatabase(),
                                            updatedDatabase,
                                            propagated));
                        }));
    }

    public Mono<Relationship> getRelationship(String id) {
        return relationshipRepository.findById(id);
    }

    public Flux<Relationship> getRelationshipsByTableId(String tableId) {
        return relationshipRepository.findByTableId(tableId);
    }

    public Mono<Relationship> updateRelationshipName(
            Validation.ChangeRelationshipNameRequest request) {
        return relationshipRepository.findById(request.getRelationshipId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .changeRelationshipName(request))
                .doOnNext(relationship -> relationship
                        .setName(request.getNewName()))
                .flatMap(relationshipRepository::save);
    }

    public Mono<Relationship> updateRelationshipCardinality(
            Validation.ChangeRelationshipCardinalityRequest request) {
        return relationshipRepository
                .findById(request.getRelationshipId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .changeRelationshipCardinality(request))
                .doOnNext(relationship -> relationship
                        .setCardinality(request.getCardinality().name()))
                .flatMap(relationshipRepository::save);
    }

    public Mono<RelationshipColumn> addColumnToRelationship(
            Validation.AddColumnToRelationshipRequest request) {
        return validationClient.addColumnToRelationship(request)
                .then(relationshipColumnRepository
                        .save(ErdMapper
                                .toEntity(request.getRelationshipColumn())));
    }

    public Mono<Void> removeColumnFromRelationship(
            Validation.RemoveColumnFromRelationshipRequest request) {
        return relationshipColumnRepository
                .findById(request.getRelationshipColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .removeColumnFromRelationship(request))
                .doOnNext(RelationshipColumn::delete)
                .flatMap(relationshipColumnRepository::save)
                .then();
    }

    public Mono<Void> deleteRelationship(
            Validation.DeleteRelationshipRequest request) {
        return relationshipRepository.findById(request.getRelationshipId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient.deleteRelationship(request))
                .doOnNext(Relationship::delete)
                .flatMap(relationshipRepository::save)
                .then();
    }

}
