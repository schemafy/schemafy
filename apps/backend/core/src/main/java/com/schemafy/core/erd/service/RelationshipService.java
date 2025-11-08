package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
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
                                            request.request().getRelationship()
                                                    .getId(),
                                            savedRelationship.getId());

                            return Flux
                                    .fromIterable(request.request()
                                            .getRelationship().getColumnsList())
                                    .flatMap(column -> {
                                        RelationshipColumn entity = ErdMapper
                                                .toEntity(column);
                                        entity.setRelationshipId(
                                                savedRelationship.getId());
                                        return relationshipColumnRepository
                                                .save(entity);
                                    })
                                    .then(affectedEntitiesSaver
                                            .saveAffectedEntities(
                                                    request.request()
                                                            .getDatabase(),
                                                    updatedDatabase,
                                                    savedRelationship.getId(),
                                                    savedRelationship.getId(),
                                                    "RELATIONSHIP"))
                                    .map(propagated -> AffectedMappingResponse
                                            .of(
                                                    request.request(),
                                                    request.request()
                                                            .getDatabase(),
                                                    updatedDatabase,
                                                    propagated));
                        }));
    }

    public Mono<RelationshipResponse> getRelationship(String id) {
        return relationshipRepository.findByIdAndDeletedAtIsNull(id)
                .flatMap(relationship -> relationshipColumnRepository
                        .findByRelationshipIdAndDeletedAtIsNull(id)
                        .collectList()
                        .map(columns -> RelationshipResponse.from(relationship,
                                columns)))
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)));
    }

    public Flux<RelationshipResponse> getRelationshipsByTableId(
            String tableId) {
        return relationshipRepository.findByTableIdAndDeletedAtIsNull(tableId)
                .flatMap(relationship -> relationshipColumnRepository
                        .findByRelationshipIdAndDeletedAtIsNull(
                                relationship.getId())
                        .collectList()
                        .map(columns -> RelationshipResponse.from(relationship,
                                columns)));
    }

    public Mono<RelationshipResponse> updateRelationshipName(
            Validation.ChangeRelationshipNameRequest request) {
        return relationshipRepository
                .findByIdAndDeletedAtIsNull(request.getRelationshipId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .changeRelationshipName(request))
                .doOnNext(relationship -> relationship
                        .setName(request.getNewName()))
                .flatMap(relationshipRepository::save)
                .map(RelationshipResponse::from);
    }

    public Mono<RelationshipResponse> updateRelationshipCardinality(
            Validation.ChangeRelationshipCardinalityRequest request) {
        return relationshipRepository
                .findByIdAndDeletedAtIsNull(request.getRelationshipId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .changeRelationshipCardinality(request))
                .doOnNext(relationship -> relationship
                        .setCardinality(request.getCardinality().name()))
                .flatMap(relationshipRepository::save)
                .map(RelationshipResponse::from);
    }

    // TODO: 여기도 전파 필요
    public Mono<RelationshipColumnResponse> addColumnToRelationship(
            Validation.AddColumnToRelationshipRequest request) {
        return validationClient.addColumnToRelationship(request)
                .then(relationshipColumnRepository.save(
                        ErdMapper.toEntity(request.getRelationshipColumn())))
                .map(RelationshipColumnResponse::from);
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
        return relationshipRepository
                .findByIdAndDeletedAtIsNull(request.getRelationshipId())
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
