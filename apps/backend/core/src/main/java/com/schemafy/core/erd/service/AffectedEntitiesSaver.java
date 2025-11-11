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

    public Mono<PropagatedEntities> saveAffectedEntities(
            Validation.Database before,
            Validation.Database after) {
        return saveAffectedEntities(before, after, null, null, null);
    }

    public Mono<PropagatedEntities> saveAffectedEntities(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType) {
        return Mono.defer(() -> {
            List<Mono<Void>> saveTasks = new ArrayList<>();

            List<PropagatedColumn> propagatedColumns = new ArrayList<>();
            List<PropagatedConstraintColumn> propagatedConstraintColumns = new ArrayList<>();
            List<PropagatedIndexColumn> propagatedIndexColumns = new ArrayList<>();

            var beforeIds = collectAllEntityIds(before);

            for (Validation.Schema schema : after.getSchemasList()) {
                if (schema.getIsAffected()
                        && !beforeIds.contains(schema.getId())
                        && !schema.getId().equals(excludeEntityId)) {
                    saveTasks.add(
                            schemaRepository.save(ErdMapper.toEntity(schema))
                                    .then());
                }

                for (Validation.Table table : schema.getTablesList()) {
                    if (table.getIsAffected()
                            && !beforeIds.contains(table.getId())
                            && !table.getId().equals(excludeEntityId)) {
                        saveTasks.add(
                                tableRepository
                                        .save(ErdMapper.toEntity(table))
                                        .then());
                    }

                    for (Validation.Column column : table.getColumnsList()) {
                        if (column.getIsAffected()
                                && !beforeIds.contains(column.getId())
                                && !column.getId().equals(excludeEntityId)) {
                            saveTasks.add(
                                    columnRepository
                                            .save(ErdMapper.toEntity(column))
                                            .then());

                            if (sourceId != null && sourceType != null) {
                                propagatedColumns.add(new PropagatedColumn(
                                        column.getId(),
                                        table.getId(),
                                        sourceType,
                                        sourceId,
                                        column.getId()));
                            }
                        }
                    }

                    for (Validation.Index index : table.getIndexesList()) {
                        if (index.getIsAffected()
                                && !beforeIds.contains(index.getId())
                                && !index.getId().equals(excludeEntityId)) {
                            saveTasks.add(
                                    indexRepository
                                            .save(ErdMapper.toEntity(index))
                                            .then());
                        }

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

                    for (Validation.Relationship relationship : table
                            .getRelationshipsList()) {
                        if (relationship.getIsAffected()
                                && !beforeIds.contains(relationship.getId())
                                && !relationship.getId()
                                        .equals(excludeEntityId)) {
                            saveTasks.add(
                                    relationshipRepository
                                            .save(ErdMapper.toEntity(
                                                    relationship))
                                            .then());
                        }

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

            return Flux.concat(saveTasks)
                    .then(Mono.just(new PropagatedEntities(
                            propagatedColumns,
                            propagatedConstraintColumns,
                            propagatedIndexColumns)));
        });
    }

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
