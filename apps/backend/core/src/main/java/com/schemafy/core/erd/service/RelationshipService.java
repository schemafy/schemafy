package com.schemafy.core.erd.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
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
    private final AffectedEntitiesSoftDeleter affectedEntitiesSoftDeleter;
    private final TransactionalOperator transactionalOperator;

    public Mono<AffectedMappingResponse> createRelationship(
            CreateRelationshipRequestWithExtra request) {
        return validationClient.createRelationship(request.request())
                .flatMap(database -> saveRelationshipWithColumns(request,
                        database));
    }

    private Mono<AffectedMappingResponse> saveRelationshipWithColumns(
            CreateRelationshipRequestWithExtra request,
            Validation.Database database) {
        return transactionalOperator.transactional(
                relationshipRepository
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

                            Set<String> excludePropagatedIds = request.request()
                                    .getRelationship()
                                    .getColumnsList()
                                    .stream()
                                    .map(Validation.RelationshipColumn::getId)
                                    .collect(Collectors.toSet());

                            return affectedEntitiesSaver
                                    .saveAffectedEntitiesResult(
                                            request.request().getDatabase(),
                                            updatedDatabase,
                                            savedRelationship.getId(),
                                            savedRelationship.getId(),
                                            EntityType.RELATIONSHIP.name(),
                                            Set.of(),
                                            excludePropagatedIds)
                                    .map(saveResult -> {
                                        Validation.Database afterDatabase = applyRelationshipColumnMappings(
                                                updatedDatabase,
                                                saveResult.idMappings()
                                                        .relationshipColumns());
                                        return AffectedMappingResponse
                                                .of(request.request(),
                                                        request.request()
                                                                .getDatabase(),
                                                        afterDatabase,
                                                        saveResult
                                                                .propagated());
                                    });
                        }));
    }

    private static Validation.Database applyRelationshipColumnMappings(
            Validation.Database database,
            Map<String, String> relationshipColumnMappings) {
        Validation.Database updatedDatabase = database;
        for (Map.Entry<String, String> mapping : relationshipColumnMappings
                .entrySet()) {
            updatedDatabase = AffectedMappingResponse
                    .updateEntityIdInDatabase(
                            updatedDatabase,
                            EntityType.RELATIONSHIP_COLUMN,
                            mapping.getKey(),
                            mapping.getValue());
        }
        return updatedDatabase;
    }

    public Mono<RelationshipResponse> getRelationship(String id) {
        return relationshipRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .flatMap(relationship -> relationshipColumnRepository
                        .findByRelationshipIdAndDeletedAtIsNull(id)
                        .collectList()
                        .map(columns -> RelationshipResponse.from(relationship,
                                columns)));
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

    public Mono<RelationshipResponse> updateRelationshipExtra(
            String relationshipId, String extra) {
        return relationshipRepository.findByIdAndDeletedAtIsNull(relationshipId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .doOnNext(relationship -> relationship.setExtra(extra))
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

    public Mono<RelationshipResponse> updateRelationshipKind(
            Validation.ChangeRelationshipKindRequest request) {
        if (!request.hasDatabase()) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return relationshipRepository
                .findByIdAndDeletedAtIsNull(request.getRelationshipId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .flatMap(relationship -> validationClient
                        .changeRelationshipKind(request)
                        .flatMap(afterDatabase -> transactionalOperator
                                .transactional(Mono.just(relationship)
                                        .doOnNext(entity -> entity.setKind(
                                                request.getKind().name()))
                                        .flatMap(relationshipRepository::save)
                                        .flatMap(
                                                savedRelationship -> affectedEntitiesSoftDeleter
                                                        .softDeleteRemovedEntities(
                                                                request.getDatabase(),
                                                                afterDatabase)
                                                        .then(affectedEntitiesSaver
                                                                .saveAffectedEntitiesResult(
                                                                        request.getDatabase(),
                                                                        afterDatabase))
                                                        .thenReturn(
                                                                savedRelationship)))))
                .map(RelationshipResponse::from);
    }

    public Mono<AffectedMappingResponse> addColumnToRelationship(
            Validation.AddColumnToRelationshipRequest request) {
        return validationClient.addColumnToRelationship(request)
                .flatMap(database -> saveRelationshipColumnAndAffectedEntities(
                        request, database));
    }

    private Mono<AffectedMappingResponse> saveRelationshipColumnAndAffectedEntities(
            Validation.AddColumnToRelationshipRequest request,
            Validation.Database database) {
        return transactionalOperator.transactional(
                relationshipColumnRepository
                        .save(ErdMapper
                                .toEntity(request.getRelationshipColumn()))
                        .flatMap(savedRelationshipColumn -> {
                            Validation.Database updatedDatabase = AffectedMappingResponse
                                    .updateEntityIdInDatabase(
                                            database,
                                            EntityType.RELATIONSHIP_COLUMN,
                                            request.getRelationshipColumn()
                                                    .getId(),
                                            savedRelationshipColumn.getId());

                            return affectedEntitiesSaver
                                    .saveAffectedEntities(
                                            request.getDatabase(),
                                            updatedDatabase,
                                            savedRelationshipColumn.getId(),
                                            request.getRelationshipId(),
                                            EntityType.RELATIONSHIP.name())
                                    .map(propagated -> AffectedMappingResponse
                                            .of(
                                                    request,
                                                    request.getDatabase(),
                                                    updatedDatabase,
                                                    propagated));
                        }));
    }

    public Mono<Void> removeColumnFromRelationship(
            Validation.RemoveColumnFromRelationshipRequest request) {
        if (!request.hasDatabase()) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return relationshipColumnRepository
                .findByIdAndDeletedAtIsNull(request.getRelationshipColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_COLUMN_NOT_FOUND)))
                .flatMap(relationshipColumn -> validationClient
                        .removeColumnFromRelationship(request)
                        .flatMap(afterDatabase -> transactionalOperator
                                .transactional(Mono.just(relationshipColumn)
                                        .doOnNext(entity -> entity.delete())
                                        .flatMap(
                                                relationshipColumnRepository::save)
                                        .then(affectedEntitiesSoftDeleter
                                                .softDeleteRemovedEntities(
                                                        request.getDatabase(),
                                                        afterDatabase))
                                        .then(affectedEntitiesSaver
                                                .saveAffectedEntitiesResult(
                                                        request.getDatabase(),
                                                        afterDatabase)
                                                .then()))))
                .then();
    }

    public Mono<Void> deleteRelationship(
            Validation.DeleteRelationshipRequest request) {
        if (!request.hasDatabase()) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return relationshipRepository
                .findByIdAndDeletedAtIsNull(request.getRelationshipId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)))
                .flatMap(relationship -> validationClient
                        .deleteRelationship(request)
                        .flatMap(afterDatabase -> transactionalOperator
                                .transactional(Mono.just(relationship)
                                        .doOnNext(entity -> entity.delete())
                                        .flatMap(relationshipRepository::save)
                                        .then(affectedEntitiesSoftDeleter
                                                .softDeleteRemovedEntities(
                                                        request.getDatabase(),
                                                        afterDatabase))
                                        .then(affectedEntitiesSaver
                                                .saveAffectedEntitiesResult(
                                                        request.getDatabase(),
                                                        afterDatabase)
                                                .then()))))
                .then();
    }

}
