package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;
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

    public Mono<AffectedMappingResponse> createConstraint(
            Validation.CreateConstraintRequest request) {
        return validationClient.createConstraint(request)
                .flatMap(database -> constraintRepository
                        .save(ErdMapper.toEntity(request.getConstraint()))
                        .flatMap(savedConstraint -> {
                            Validation.Database updatedDatabase = AffectedMappingResponse
                                    .updateEntityIdInDatabase(
                                            database,
                                            EntityType.CONSTRAINT,
                                            request.getConstraint().getId(),
                                            savedConstraint.getId());

                            return Flux
                                    .fromIterable(request.getConstraint()
                                            .getColumnsList())
                                    .flatMap(column -> {
                                        ConstraintColumn entity = ErdMapper
                                                .toEntity(column);
                                        entity.setConstraintId(
                                                savedConstraint.getId());
                                        return constraintColumnRepository
                                                .save(entity);
                                    })
                                    .then(affectedEntitiesSaver
                                            .saveAffectedEntities(
                                                    request.getDatabase(),
                                                    updatedDatabase,
                                                    savedConstraint.getId(),
                                                    savedConstraint.getId(),
                                                    "CONSTRAINT"))
                                    .map(propagated -> AffectedMappingResponse
                                            .of(
                                                    request,
                                                    request.getDatabase(),
                                                    updatedDatabase,
                                                    propagated));
                        }));
    }

    public Mono<ConstraintResponse> getConstraint(String id) {
        return constraintRepository.findByIdAndDeletedAtIsNull(id)
                .flatMap(constraint -> constraintColumnRepository
                        .findByConstraintIdAndDeletedAtIsNull(id)
                        .collectList()
                        .map(columns -> ConstraintResponse.from(constraint,
                                columns)))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_CONSTRAINT_NOT_FOUND)));
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
                        ignore -> validationClient.changeConstraintName(request))
                .doOnNext(
                        constraint -> constraint.setName(request.getNewName()))
                .flatMap(constraintRepository::save)
                .map(ConstraintResponse::from);
    }

    // TODO: 여기도 전파 필요
    public Mono<ConstraintColumnResponse> addColumnToConstraint(
            Validation.AddColumnToConstraintRequest request) {
        return validationClient.addColumnToConstraint(request)
                .then(constraintColumnRepository.save(
                        ErdMapper.toEntity(request.getConstraintColumn())))
                .map(ConstraintColumnResponse::from);
    }

    public Mono<Void> removeColumnFromConstraint(
            Validation.RemoveColumnFromConstraintRequest request) {
        return constraintColumnRepository
                .findByIdAndDeletedAtIsNull(request.getConstraintColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_CONSTRAINT_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .removeColumnFromConstraint(request))
                .doOnNext(ConstraintColumn::delete)
                .flatMap(constraintColumnRepository::save)
                .then();
    }

    public Mono<Void> deleteConstraint(
            Validation.DeleteConstraintRequest request) {
        return constraintRepository
                .findByIdAndDeletedAtIsNull(request.getConstraintId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_CONSTRAINT_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient.deleteConstraint(request))
                .doOnNext(Constraint::delete)
                .flatMap(constraintRepository::save)
                .then();
    }

}
