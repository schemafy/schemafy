package com.schemafy.core.erd.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@Service
@RequiredArgsConstructor
public class ConstraintService {

    private final ValidationClient validationClient;
    private final ConstraintRepository constraintRepository;
    private final ConstraintColumnRepository constraintColumnRepository;
    private final AffectedEntitiesSaver affectedEntitiesSaver;
    private final AffectedEntitiesSoftDeleter affectedEntitiesSoftDeleter;
    private final TransactionalOperator transactionalOperator;

    public Mono<AffectedMappingResponse> createConstraint(
            Validation.CreateConstraintRequest request) {
        return validationClient.createConstraint(request)
                .flatMap(database -> saveConstraintWithColumns(request,
                        database));
    }

    private Mono<AffectedMappingResponse> saveConstraintWithColumns(
            Validation.CreateConstraintRequest request,
            Validation.Database database) {
        return transactionalOperator.transactional(
                constraintRepository
                        .save(ErdMapper.toEntity(request.getConstraint()))
                        .flatMap(savedConstraint -> {
                            Validation.Database updatedDatabase = AffectedMappingResponse
                                    .updateEntityIdInDatabase(
                                            database,
                                            EntityType.CONSTRAINT,
                                            request.getConstraint().getId(),
                                            savedConstraint.getId());

                            Set<String> excludePropagatedIds = new HashSet<>(
                                    request.getConstraint().getColumnsList()
                                            .stream()
                                            .map(Validation.ConstraintColumn::getId)
                                            .collect(Collectors.toSet()));
                            excludePropagatedIds.add(savedConstraint.getId());

                            return affectedEntitiesSaver
                                    .saveAffectedEntitiesResult(
                                            request.getDatabase(),
                                            updatedDatabase,
                                            savedConstraint.getId(),
                                            savedConstraint.getId(),
                                            EntityType.CONSTRAINT.name(),
                                            Set.of(),
                                            excludePropagatedIds)
                                    .map(saveResult -> {
                                        Validation.Database afterDatabase = applyConstraintColumnMappings(
                                                updatedDatabase,
                                                saveResult.idMappings()
                                                        .constraintColumns());
                                        return AffectedMappingResponse.of(
                                                request,
                                                request.getDatabase(),
                                                afterDatabase,
                                                saveResult.propagated());
                                    });
                        }));
    }

    private static Validation.Database applyConstraintColumnMappings(
            Validation.Database database,
            Map<String, String> constraintColumnMappings) {
        Validation.Database updatedDatabase = database;
        for (Map.Entry<String, String> mapping : constraintColumnMappings
                .entrySet()) {
            updatedDatabase = AffectedMappingResponse
                    .updateEntityIdInDatabase(
                            updatedDatabase,
                            EntityType.CONSTRAINT_COLUMN,
                            mapping.getKey(),
                            mapping.getValue());
        }
        return updatedDatabase;
    }

    public Mono<ConstraintResponse> getConstraint(String id) {
        return constraintRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_CONSTRAINT_NOT_FOUND)))
                .flatMap(constraint -> constraintColumnRepository
                        .findByConstraintIdAndDeletedAtIsNull(id)
                        .collectList()
                        .map(columns -> ConstraintResponse.from(constraint,
                                columns)));
    }

    public Flux<ConstraintResponse> getConstraintsByTableId(String tableId) {
        return constraintRepository.findByTableIdAndDeletedAtIsNull(tableId)
                .flatMap(constraint -> constraintColumnRepository
                        .findByConstraintIdAndDeletedAtIsNull(
                                constraint.getId())
                        .collectList()
                        .map(columns -> ConstraintResponse.from(constraint,
                                columns)));
    }

    public Mono<ConstraintResponse> updateConstraintName(
            Validation.ChangeConstraintNameRequest request) {
        return constraintRepository
                .findByIdAndDeletedAtIsNull(request.getConstraintId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_CONSTRAINT_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient
                                .changeConstraintName(request))
                .doOnNext(
                        constraint -> constraint.setName(request.getNewName()))
                .flatMap(constraintRepository::save)
                .map(ConstraintResponse::from);
    }

    public Mono<AffectedMappingResponse> addColumnToConstraint(
            Validation.AddColumnToConstraintRequest request) {
        return validationClient.addColumnToConstraint(request)
                .flatMap(database -> saveConstraintColumnAndAffectedEntities(
                        request, database));
    }

    private Mono<AffectedMappingResponse> saveConstraintColumnAndAffectedEntities(
            Validation.AddColumnToConstraintRequest request,
            Validation.Database database) {
        return transactionalOperator.transactional(
                constraintColumnRepository
                        .save(ErdMapper.toEntity(request.getConstraintColumn()))
                        .flatMap(savedConstraintColumn -> {
                            Validation.Database updatedDatabase = AffectedMappingResponse
                                    .updateEntityIdInDatabase(
                                            database,
                                            EntityType.CONSTRAINT_COLUMN,
                                            request.getConstraintColumn()
                                                    .getId(),
                                            savedConstraintColumn.getId());

                            return affectedEntitiesSaver
                                    .saveAffectedEntities(
                                            request.getDatabase(),
                                            updatedDatabase,
                                            savedConstraintColumn.getId(),
                                            request.getConstraintId(),
                                            EntityType.CONSTRAINT.name())
                                    .map(propagated -> AffectedMappingResponse
                                            .of(
                                                    request,
                                                    request.getDatabase(),
                                                    updatedDatabase,
                                                    propagated));
                        }));
    }

    public Mono<Void> removeColumnFromConstraint(
            Validation.RemoveColumnFromConstraintRequest request) {
        if (!request.hasDatabase()) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return constraintColumnRepository
                .findByIdAndDeletedAtIsNull(request.getConstraintColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_CONSTRAINT_COLUMN_NOT_FOUND)))
                .flatMap(constraintColumn -> validationClient
                        .removeColumnFromConstraint(request)
                        .flatMap(afterDatabase -> transactionalOperator
                                .transactional(Mono.just(constraintColumn)
                                        .doOnNext(entity -> entity.delete())
                                        .flatMap(
                                                constraintColumnRepository::save)
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

    public Mono<Void> deleteConstraint(
            Validation.DeleteConstraintRequest request) {
        if (!request.hasDatabase()) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return constraintRepository
                .findByIdAndDeletedAtIsNull(request.getConstraintId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_CONSTRAINT_NOT_FOUND)))
                .flatMap(constraint -> validationClient
                        .deleteConstraint(request)
                        .flatMap(afterDatabase -> transactionalOperator
                                .transactional(Mono.just(constraint)
                                        .doOnNext(entity -> entity.delete())
                                        .flatMap(constraintRepository::save)
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
