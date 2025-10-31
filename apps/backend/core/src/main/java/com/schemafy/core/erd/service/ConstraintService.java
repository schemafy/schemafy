package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation.CreateConstraintRequest;

@Service
@RequiredArgsConstructor
public class ConstraintService {

    private final ValidationClient validationClient;
    private final ConstraintRepository constraintRepository;
    private final ConstraintColumnRepository constraintColumnRepository;
    private final AffectedEntitiesSaver affectedEntitiesSaver;

    public Mono<AffectedMappingResponse> createConstraint(
            CreateConstraintRequest request) {
        return validationClient.createConstraint(request)
                .flatMap(database -> {
                    String constraintId = request.getConstraint().getId();

                    // 요청한 Constraint 저장
                    Mono<Constraint> saveConstraint = constraintRepository
                            .save(ErdMapper.toEntity(request.getConstraint()));

                    // 영향받은 엔티티 저장 + 전파 정보 수집
                    // PK 제약조건인 경우 하위 식별 관계 전파
                    return saveConstraint
                            .then(affectedEntitiesSaver.saveAffectedEntities(
                                    request.getDatabase(),
                                    database,
                                    constraintId,
                                    constraintId,      // sourceId
                                    "CONSTRAINT"       // sourceType
                    ))
                            .map(propagated -> AffectedMappingResponse.of(
                                    request,
                                    request.getDatabase(),
                                    database,
                                    propagated));
                });
    }

    public Mono<Constraint> getConstraint(String id) {
        return constraintRepository.findById(id);
    }

    public Flux<Constraint> getConstraintsByTableId(String tableId) {
        return constraintRepository.findByTableId(tableId);
    }

    public Mono<Constraint> updateConstraintName(
            validation.Validation.ChangeConstraintNameRequest request) {
        return constraintRepository.findById(request.getConstraintId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_CONSTRAINT_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .changeConstraintName(request))
                .doOnNext(
                        constraint -> constraint.setName(request.getNewName()))
                .flatMap(constraintRepository::save);
    }

    public Mono<ConstraintColumn> addColumnToConstraint(
            validation.Validation.AddColumnToConstraintRequest request) {
        return validationClient.addColumnToConstraint(request)
                .then(constraintColumnRepository
                        .save(ErdMapper
                                .toEntity(request.getConstraintColumn())));
    }

    public Mono<Void> removeColumnFromConstraint(
            validation.Validation.RemoveColumnFromConstraintRequest request) {
        return constraintColumnRepository
                .findById(request.getConstraintColumnId())
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
            validation.Validation.DeleteConstraintRequest request) {
        return constraintRepository.findById(request.getConstraintId())
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
