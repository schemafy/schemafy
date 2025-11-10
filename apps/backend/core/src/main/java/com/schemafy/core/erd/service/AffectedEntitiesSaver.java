package com.schemafy.core.erd.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedColumn;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedConstraintColumn;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedEntities;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedIndexColumn;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.TableRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

/**
 * ValidationClient가 반환한 Database 객체에서 영향받은 엔티티들을 저장하는 유틸리티
 *
 * <p>isAffected=true인 모든 엔티티를 데이터베이스에 저장합니다.
 * 주로 PK Constraint 생성이나 식별 관계(Identifying Relationship) 생성 시
 * 하위 테이블로 전파된 엔티티들을 저장하는 데 사용됩니다.
 *
 * <p>저장 순서 (계층 구조 따름):
 * <ol>
 *   <li>Schemas</li>
 *   <li>Tables</li>
 *   <li>Columns</li>
 *   <li>Indexes</li>
 *   <li>Index Columns</li>
 *   <li>Constraints</li>
 *   <li>Constraint Columns</li>
 *   <li>Relationships</li>
 *   <li>Relationship Columns</li>
 * </ol>
 *
 * @see com.schemafy.core.erd.mapper.ErdMapper
 */
@Component
@RequiredArgsConstructor
public class AffectedEntitiesSaver {

    private final SchemaRepository schemaRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final IndexRepository indexRepository;
    private final IndexColumnRepository indexColumnRepository;
    private final ConstraintRepository constraintRepository;
    private final ConstraintColumnRepository constraintColumnRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipColumnRepository relationshipColumnRepository;

    /**
     * Before와 After Database를 비교하여 새롭게 생성된 엔티티만 데이터베이스에 저장합니다.
     *
     * <p>생성 작업에서 전파로 인해 새롭게 추가된 엔티티들을 저장합니다.
     * isAffected=true이지만 Before에도 존재하는 엔티티는 제외합니다 (기존 데이터 보존).
     *
     * @param before 작업 이전의 Database 상태
     * @param after 작업 이후의 Database 상태 (ValidationClient 반환)
     * @return 모든 저장 작업이 완료되면 완료되는 Mono
     */
    public Mono<PropagatedEntities> saveAffectedEntities(
            Validation.Database before,
            Validation.Database after) {
        return saveAffectedEntities(before, after, null, null, null);
    }

    /**
     * Before와 After Database를 비교하여 새롭게 생성된 엔티티만 데이터베이스에 저장합니다.
     * 요청한 엔티티는 Service에서 이미 저장하므로 제외합니다.
     *
     * @param before 작업 이전의 Database 상태
     * @param after 작업 이후의 Database 상태 (ValidationClient 반환)
     * @param excludeEntityId Service에서 이미 저장한 엔티티 ID (중복 저장 방지)
     * @param sourceId 전파 출처 엔티티의 ID (Relationship 또는 Constraint)
     * @param sourceType 전파 출처 타입 ("RELATIONSHIP" 또는 "CONSTRAINT")
     * @return 전파된 엔티티 정보를 포함하는 PropagatedEntities
     */
    public Mono<PropagatedEntities> saveAffectedEntities(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType) {
        return Mono.defer(() -> {
            List<Mono<Void>> saveTasks = new ArrayList<>();

            // 전파된 엔티티 정보 수집용 리스트
            List<PropagatedColumn> propagatedColumns = new ArrayList<>();
            List<PropagatedConstraintColumn> propagatedConstraintColumns = new ArrayList<>();
            List<PropagatedIndexColumn> propagatedIndexColumns = new ArrayList<>();

            // Before의 모든 엔티티 ID를 Set으로 수집 (빠른 조회)
            var beforeIds = collectAllEntityIds(before);

            // Schema 순회
            for (Validation.Schema schema : after.getSchemasList()) {
                if (schema.getIsAffected()
                        && !beforeIds.contains(schema.getId())
                        && !schema.getId().equals(excludeEntityId)) {
                    saveTasks.add(
                            schemaRepository.save(ErdMapper.toEntity(schema))
                                    .then());
                }

                // Table 순회
                for (Validation.Table table : schema.getTablesList()) {
                    if (table.getIsAffected()
                            && !beforeIds.contains(table.getId())
                            && !table.getId().equals(excludeEntityId)) {
                        // 새로 생성된 Table은 extra가 없음
                        saveTasks.add(
                                tableRepository
                                        .save(ErdMapper.toEntity(table))
                                        .then());
                    }

                    // Column 순회
                    for (Validation.Column column : table.getColumnsList()) {
                        if (column.getIsAffected()
                                && !beforeIds.contains(column.getId())
                                && !column.getId().equals(excludeEntityId)) {
                            saveTasks.add(
                                    columnRepository
                                            .save(ErdMapper.toEntity(column))
                                            .then());

                            // 전파된 컬럼 정보 수집 (sourceId와 sourceType이 있는 경우)
                            if (sourceId != null && sourceType != null) {
                                propagatedColumns.add(new PropagatedColumn(
                                        column.getId(),
                                        table.getId(),
                                        sourceType,
                                        sourceId,
                                        column.getId() // sourceColumnId는 현재 컬럼 ID (추후 개선 가능)
                                ));
                            }
                        }
                    }

                    // Index 순회
                    for (Validation.Index index : table.getIndexesList()) {
                        if (index.getIsAffected()
                                && !beforeIds.contains(index.getId())
                                && !index.getId().equals(excludeEntityId)) {
                            saveTasks.add(
                                    indexRepository
                                            .save(ErdMapper.toEntity(index))
                                            .then());
                        }

                        // Index Columns 순회
                        for (Validation.IndexColumn indexColumn : index
                                .getColumnsList()) {
                            if (indexColumn.getIsAffected()
                                    && !beforeIds.contains(indexColumn.getId())
                                    && !indexColumn.getId()
                                            .equals(excludeEntityId)) {
                                saveTasks.add(
                                        indexColumnRepository
                                                .save(ErdMapper
                                                        .toEntity(indexColumn))
                                                .then());

                                // 전파된 인덱스 컬럼 정보 수집
                                if (sourceId != null && sourceType != null) {
                                    propagatedIndexColumns
                                            .add(new PropagatedIndexColumn(
                                                    indexColumn.getId(),
                                                    index.getId(),
                                                    indexColumn.getColumnId(),
                                                    sourceType,
                                                    sourceId));
                                }
                            }
                        }
                    }

                    // Constraint 순회
                    for (Validation.Constraint constraint : table
                            .getConstraintsList()) {
                        if (constraint.getIsAffected()
                                && !beforeIds.contains(constraint.getId())
                                && !constraint.getId()
                                        .equals(excludeEntityId)) {
                            saveTasks.add(
                                    constraintRepository
                                            .save(ErdMapper
                                                    .toEntity(constraint))
                                            .then());
                        }

                        // Constraint Columns 순회
                        for (Validation.ConstraintColumn constraintColumn : constraint
                                .getColumnsList()) {
                            if (constraintColumn.getIsAffected()
                                    && !beforeIds
                                            .contains(constraintColumn.getId())
                                    && !constraintColumn.getId()
                                            .equals(excludeEntityId)) {
                                saveTasks.add(
                                        constraintColumnRepository
                                                .save(ErdMapper.toEntity(
                                                        constraintColumn))
                                                .then());

                                // 전파된 제약조건 컬럼 정보 수집
                                if (sourceId != null && sourceType != null) {
                                    propagatedConstraintColumns
                                            .add(new PropagatedConstraintColumn(
                                                    constraintColumn.getId(),
                                                    constraint.getId(),
                                                    constraintColumn
                                                            .getColumnId(),
                                                    sourceType,
                                                    sourceId));
                                }
                            }
                        }
                    }

                    // Relationship 순회
                    for (Validation.Relationship relationship : table
                            .getRelationshipsList()) {
                        if (relationship.getIsAffected()
                                && !beforeIds.contains(relationship.getId())
                                && !relationship.getId()
                                        .equals(excludeEntityId)) {
                            // 새로 생성된 Relationship은 extra가 없음
                            saveTasks.add(
                                    relationshipRepository
                                            .save(ErdMapper.toEntity(
                                                    relationship))
                                            .then());
                        }

                        // Relationship Columns 순회
                        for (Validation.RelationshipColumn relationshipColumn : relationship
                                .getColumnsList()) {
                            if (relationshipColumn.getIsAffected()
                                    && !beforeIds.contains(
                                            relationshipColumn.getId())
                                    && !relationshipColumn.getId()
                                            .equals(excludeEntityId)) {
                                saveTasks.add(
                                        relationshipColumnRepository
                                                .save(ErdMapper.toEntity(
                                                        relationshipColumn))
                                                .then());
                            }
                        }
                    }
                }
            }

            // 모든 저장 작업을 순차적으로 실행 (Flux.concat)
            // 계층 구조를 따라 순서대로 저장되므로 FK 제약조건 위반 방지
            return Flux.concat(saveTasks)
                    .then(Mono.just(new PropagatedEntities(
                            propagatedColumns,
                            propagatedConstraintColumns,
                            propagatedIndexColumns)));
        });
    }

    /**
     * Database에 포함된 모든 엔티티의 ID를 수집합니다.
     * Before Database와 비교하여 새로운 엔티티를 찾는 데 사용됩니다.
     *
     * @param database ID를 수집할 Database 객체
     * @return 모든 엔티티 ID의 Set
     */
    private java.util.Set<String> collectAllEntityIds(
            Validation.Database database) {
        java.util.Set<String> ids = new java.util.HashSet<>();

        for (Validation.Schema schema : database.getSchemasList()) {
            ids.add(schema.getId());

            for (Validation.Table table : schema.getTablesList()) {
                ids.add(table.getId());

                for (Validation.Column column : table.getColumnsList()) {
                    ids.add(column.getId());
                }

                for (Validation.Index index : table.getIndexesList()) {
                    ids.add(index.getId());

                    for (Validation.IndexColumn indexColumn : index
                            .getColumnsList()) {
                        ids.add(indexColumn.getId());
                    }
                }

                for (Validation.Constraint constraint : table
                        .getConstraintsList()) {
                    ids.add(constraint.getId());

                    for (Validation.ConstraintColumn constraintColumn : constraint
                            .getColumnsList()) {
                        ids.add(constraintColumn.getId());
                    }
                }

                for (Validation.Relationship relationship : table
                        .getRelationshipsList()) {
                    ids.add(relationship.getId());

                    for (Validation.RelationshipColumn relationshipColumn : relationship
                            .getColumnsList()) {
                        ids.add(relationshipColumn.getId());
                    }
                }
            }
        }

        return ids;
    }

}
