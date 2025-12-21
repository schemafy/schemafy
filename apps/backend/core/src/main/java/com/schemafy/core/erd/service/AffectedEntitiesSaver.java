package com.schemafy.core.erd.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedColumn;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedConstraint;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedConstraintColumn;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedEntities;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedIndexColumn;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse.PropagatedRelationshipColumn;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.TableRepository;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;

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
        return saveAffectedEntities(before, after, null, null, null, Set.of());
    }

    public Mono<PropagatedEntities> saveAffectedEntities(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType) {
        return saveAffectedEntities(before, after, excludeEntityId, sourceId,
                sourceType, Set.of());
    }

    public Mono<PropagatedEntities> saveAffectedEntities(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType,
            Set<String> excludeEntityIds) {
        return saveAffectedEntitiesResult(before, after, excludeEntityId,
                sourceId,
                sourceType, excludeEntityIds, Set.of())
                .map(SaveResult::propagated);
    }

    public Mono<PropagatedEntities> saveAffectedEntities(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType,
            Set<String> excludeEntityIds,
            Set<String> excludePropagatedEntityIds) {
        return saveAffectedEntitiesResult(before, after, excludeEntityId,
                sourceId,
                sourceType, excludeEntityIds, excludePropagatedEntityIds)
                .map(SaveResult::propagated);
    }

    public Mono<SaveResult> saveAffectedEntitiesResult(
            Validation.Database before,
            Validation.Database after) {
        return saveAffectedEntitiesResult(before, after, null, null, null,
                Set.of());
    }

    public Mono<SaveResult> saveAffectedEntitiesResult(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType) {
        return saveAffectedEntitiesResult(before, after, excludeEntityId,
                sourceId,
                sourceType, Set.of());
    }

    public Mono<SaveResult> saveAffectedEntitiesResult(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType,
            Set<String> excludeEntityIds) {
        return saveAffectedEntitiesResult(before, after, excludeEntityId,
                sourceId,
                sourceType, excludeEntityIds, Set.of());
    }

    public Mono<SaveResult> saveAffectedEntitiesResult(
            Validation.Database before,
            Validation.Database after,
            String excludeEntityId,
            String sourceId,
            String sourceType,
            Set<String> excludeEntityIds,
            Set<String> excludePropagatedEntityIds) {
        return Mono.defer(() -> {
            Set<String> excludedIds = new HashSet<>();
            if (excludeEntityId != null) {
                excludedIds.add(excludeEntityId);
            }
            if (excludeEntityIds != null) {
                excludedIds.addAll(excludeEntityIds);
            }
            Set<String> excludedPropagatedIds = excludePropagatedEntityIds != null
                    ? excludePropagatedEntityIds
                    : Set.of();

            List<PropagatedColumn> propagatedColumns = new ArrayList<>();
            List<PropagatedRelationshipColumn> propagatedRelationshipColumns = new ArrayList<>();
            List<PropagatedConstraint> propagatedConstraints = new ArrayList<>();
            List<PropagatedConstraintColumn> propagatedConstraintColumns = new ArrayList<>();
            List<PropagatedIndexColumn> propagatedIndexColumns = new ArrayList<>();
            List<SavedPropagatedColumn> savedPropagatedColumns = new ArrayList<>();

            Map<String, String> schemaIdMap = new HashMap<>();
            Map<String, String> tableIdMap = new HashMap<>();
            Map<String, String> columnIdMap = new HashMap<>();
            Map<String, String> indexIdMap = new HashMap<>();
            Map<String, String> indexColumnIdMap = new HashMap<>();
            Map<String, String> constraintIdMap = new HashMap<>();
            Map<String, String> constraintColumnIdMap = new HashMap<>();
            Map<String, String> relationshipIdMap = new HashMap<>();
            Map<String, String> relationshipColumnIdMap = new HashMap<>();

            List<Validation.Schema> schemasToSave = new ArrayList<>();
            List<Validation.Table> tablesToSave = new ArrayList<>();
            List<Validation.Column> columnsToSave = new ArrayList<>();
            List<Validation.Index> indexesToSave = new ArrayList<>();
            List<Validation.IndexColumn> indexColumnsToSave = new ArrayList<>();
            List<Validation.Constraint> constraintsToSave = new ArrayList<>();
            List<ConstraintColumnWithConstraintId> constraintColumnsToSave = new ArrayList<>();
            List<Validation.Relationship> relationshipsToSave = new ArrayList<>();
            List<RelationshipColumnWithRelationshipId> relationshipColumnsToSave = new ArrayList<>();

            var beforeIds = collectAllEntityIds(before);

            for (Validation.Schema schema : after.getSchemasList()) {
                if (shouldSaveEntity(schema.getId(), schema.getIsAffected(),
                        beforeIds, excludedIds)) {
                    schemasToSave.add(schema);
                }

                for (Validation.Table table : schema.getTablesList()) {
                    if (shouldSaveEntity(table.getId(), table.getIsAffected(),
                            beforeIds, excludedIds)) {
                        tablesToSave.add(table);
                    }

                    for (Validation.Column column : table.getColumnsList()) {
                        if (shouldSaveEntity(column.getId(),
                                column.getIsAffected(), beforeIds,
                                excludedIds)) {
                            columnsToSave.add(column);
                        }
                    }

                    for (Validation.Index index : table.getIndexesList()) {
                        if (shouldSaveEntity(index.getId(), index.getIsAffected(),
                                beforeIds, excludedIds)) {
                            indexesToSave.add(index);
                        }

                        for (Validation.IndexColumn indexColumn : index
                                .getColumnsList()) {
                            if (shouldSaveEntity(indexColumn.getId(),
                                    indexColumn.getIsAffected(), beforeIds,
                                    excludedIds)) {
                                indexColumnsToSave.add(indexColumn);
                            }
                        }
                    }

                    for (Validation.Constraint constraint : table
                            .getConstraintsList()) {
                        if (shouldSaveEntity(constraint.getId(),
                                constraint.getIsAffected(), beforeIds,
                                excludedIds)) {
                            constraintsToSave.add(constraint);
                        }

                        for (Validation.ConstraintColumn constraintColumn : constraint
                                .getColumnsList()) {
                            if (shouldSaveEntity(constraintColumn.getId(),
                                    constraintColumn.getIsAffected(), beforeIds,
                                    excludedIds)) {
                                constraintColumnsToSave
                                        .add(new ConstraintColumnWithConstraintId(
                                                constraintColumn,
                                                constraint.getId()));
                            }
                        }
                    }

                    for (Validation.Relationship relationship : table
                            .getRelationshipsList()) {
                        if (shouldSaveEntity(relationship.getId(),
                                relationship.getIsAffected(), beforeIds,
                                excludedIds)) {
                            relationshipsToSave.add(relationship);
                        }

                        for (Validation.RelationshipColumn relationshipColumn : relationship
                                .getColumnsList()) {
                            if (shouldSaveEntity(relationshipColumn.getId(),
                                    relationshipColumn.getIsAffected(), beforeIds,
                                    excludedIds)) {
                                relationshipColumnsToSave
                                        .add(new RelationshipColumnWithRelationshipId(
                                                relationshipColumn,
                                                relationship.getId()));
                            }
                        }
                    }
                }
            }

            Mono<Void> saveSchemasTask = Flux.fromIterable(schemasToSave)
                    .concatMap(schema -> schemaRepository
                            .save(ErdMapper.toEntity(schema))
                            .doOnNext(savedSchema -> schemaIdMap
                                    .put(schema.getId(), savedSchema.getId()))
                            .then())
                    .then();

            Mono<Void> saveTablesTask = saveSchemasTask
                    .thenMany(Flux.fromIterable(tablesToSave)
                            .concatMap(table -> {
                                var entity = ErdMapper.toEntity(table);
                                entity.setSchemaId(remapId(table.getSchemaId(),
                                        schemaIdMap));
                                return tableRepository.save(entity)
                                        .doOnNext(savedTable -> tableIdMap
                                                .put(table.getId(),
                                                        savedTable.getId()))
                                        .then();
                            }))
                    .then();

            Mono<Void> saveColumnsTask = saveTablesTask
                    .thenMany(Flux.fromIterable(columnsToSave)
                            .concatMap(column -> {
                                var entity = ErdMapper.toEntity(column);
                                entity.setTableId(remapId(column.getTableId(),
                                        tableIdMap));
                                return columnRepository.save(entity)
                                        .doOnNext(savedColumn -> {
                                            columnIdMap.put(column.getId(),
                                                    savedColumn.getId());

                                            if (sourceId != null
                                                    && sourceType != null
                                                    && !excludedPropagatedIds
                                                            .contains(
                                                                    column.getId())) {
                                                savedPropagatedColumns.add(
                                                        new SavedPropagatedColumn(
                                                                savedColumn
                                                                        .getId(),
                                                                entity
                                                                        .getTableId()));
                                            }
                                        })
                                        .then();
                            }))
                    .then();

            Mono<Void> populatePropagatedColumnsTask = saveColumnsTask.then(
                    Mono.fromRunnable(() -> {
                        if (sourceId == null || sourceType == null) {
                            return;
                        }

                        Map<String, String> fkToRefColumnIdMap = buildFkToRefColumnIdMap(
                                after,
                                sourceType,
                                sourceId,
                                columnIdMap);

                        for (SavedPropagatedColumn savedColumn : savedPropagatedColumns) {
                            String sourceColumnId = fkToRefColumnIdMap
                                    .getOrDefault(savedColumn.columnId(),
                                            savedColumn.columnId());
                            propagatedColumns.add(new PropagatedColumn(
                                    savedColumn.columnId(),
                                    savedColumn.tableId(),
                                    sourceType,
                                    sourceId,
                                    sourceColumnId));
                        }
                    }));

            Mono<Void> saveIndexesTask = populatePropagatedColumnsTask
                    .thenMany(Flux.fromIterable(indexesToSave)
                            .concatMap(index -> {
                                var entity = ErdMapper.toEntity(index);
                                entity.setTableId(remapId(index.getTableId(),
                                        tableIdMap));
                                return indexRepository.save(entity)
                                        .doOnNext(savedIndex -> indexIdMap.put(
                                                index.getId(),
                                                savedIndex.getId()))
                                        .then();
                            }))
                    .then();

            Mono<Void> saveConstraintsTask = saveIndexesTask
                    .thenMany(Flux.fromIterable(constraintsToSave)
                            .concatMap(constraint -> {
                                var entity = ErdMapper.toEntity(constraint);
                                entity.setTableId(remapId(
                                        constraint.getTableId(),
                                        tableIdMap));
                                return constraintRepository.save(entity)
                                        .doOnNext(savedConstraint -> {
                                            constraintIdMap.put(
                                                    constraint.getId(),
                                                    savedConstraint.getId());

                                            if (sourceId != null
                                                    && sourceType != null
                                                    && !excludedPropagatedIds
                                                            .contains(constraint
                                                                    .getId())) {
                                                propagatedConstraints
                                                        .add(new PropagatedConstraint(
                                                                savedConstraint
                                                                        .getId(),
                                                                entity
                                                                        .getTableId(),
                                                                entity
                                                                        .getName(),
                                                                entity
                                                                        .getKind(),
                                                                sourceType,
                                                                sourceId));
                                            }
                                        })
                                        .then();
                            }))
                    .then();

            Mono<Void> saveRelationshipsTask = saveConstraintsTask
                    .thenMany(Flux.fromIterable(relationshipsToSave)
                            .concatMap(relationship -> {
                                var entity = ErdMapper
                                        .toEntity(relationship);
                                entity.setSrcTableId(remapId(
                                        relationship.getSrcTableId(),
                                        tableIdMap));
                                entity.setTgtTableId(remapId(
                                        relationship.getTgtTableId(),
                                        tableIdMap));
                                return relationshipRepository.save(entity)
                                        .doOnNext(savedRelationship -> relationshipIdMap
                                                .put(relationship.getId(),
                                                        savedRelationship
                                                                .getId()))
                                        .then();
                            }))
                    .then();

            Mono<Void> saveIndexColumnsTask = saveRelationshipsTask
                    .thenMany(Flux.fromIterable(indexColumnsToSave)
                            .concatMap(indexColumn -> {
                                var entity = ErdMapper.toEntity(indexColumn);
                                entity.setIndexId(remapId(
                                        indexColumn.getIndexId(),
                                        indexIdMap));
                                entity.setColumnId(remapId(
                                        indexColumn.getColumnId(),
                                        columnIdMap));

                                return indexColumnRepository.save(entity)
                                        .doOnNext(savedIndexColumn -> {
                                            indexColumnIdMap.put(
                                                    indexColumn.getId(),
                                                    savedIndexColumn.getId());
                                            if (sourceId != null
                                                    && sourceType != null
                                                    && !excludedPropagatedIds
                                                            .contains(indexColumn
                                                                    .getId())) {
                                                propagatedIndexColumns
                                                        .add(new PropagatedIndexColumn(
                                                                savedIndexColumn
                                                                        .getId(),
                                                                entity
                                                                        .getIndexId(),
                                                                entity
                                                                        .getColumnId(),
                                                                sourceType,
                                                                sourceId));
                                            }
                                        })
                                        .then();
                            }))
                    .then();

            Mono<Void> saveConstraintColumnsTask = saveIndexColumnsTask
                    .thenMany(Flux.fromIterable(constraintColumnsToSave)
                            .concatMap(item -> {
                                Validation.ConstraintColumn constraintColumn = item
                                        .constraintColumn();

                                var entity = ErdMapper
                                        .toEntity(constraintColumn);
                                entity.setConstraintId(remapId(
                                        item.constraintId(),
                                        constraintIdMap));
                                entity.setColumnId(remapId(
                                        constraintColumn.getColumnId(),
                                        columnIdMap));

                                return constraintColumnRepository.save(entity)
                                        .doOnNext(savedConstraintColumn -> {
                                            constraintColumnIdMap.put(
                                                    constraintColumn.getId(),
                                                    savedConstraintColumn
                                                            .getId());
                                            if (sourceId != null
                                                    && sourceType != null
                                                    && !excludedPropagatedIds
                                                            .contains(constraintColumn
                                                                    .getId())) {
                                                propagatedConstraintColumns
                                                        .add(new PropagatedConstraintColumn(
                                                                savedConstraintColumn
                                                                        .getId(),
                                                                entity
                                                                        .getConstraintId(),
                                                                entity
                                                                        .getColumnId(),
                                                                sourceType,
                                                                sourceId));
                                            }
                                        })
                                        .then();
                            }))
                    .then();

            Mono<Void> saveRelationshipColumnsTask = saveConstraintColumnsTask
                    .thenMany(Flux.fromIterable(relationshipColumnsToSave)
                            .concatMap(item -> {
                                Validation.RelationshipColumn relationshipColumn = item.relationshipColumn();

                                String relationshipId = remapId(
                                        item.relationshipId(),
                                        relationshipIdMap);
                                String fkColumnId = remapId(
                                        relationshipColumn.getFkColumnId(),
                                        columnIdMap);
                                String refColumnId = remapId(
                                        relationshipColumn.getRefColumnId(),
                                        columnIdMap);

                                RelationshipColumn entity = RelationshipColumn
                                        .builder()
                                        .relationshipId(relationshipId)
                                        .srcColumnId(fkColumnId)
                                        .tgtColumnId(refColumnId)
                                        .seqNo((int) relationshipColumn
                                                .getSeqNo())
                                        .build();

                                return relationshipColumnRepository.save(entity)
                                        .doOnNext(savedRelationshipColumn -> {
                                            relationshipColumnIdMap.put(
                                                    relationshipColumn.getId(),
                                                    savedRelationshipColumn
                                                            .getId());
                                            if (sourceId != null
                                                    && sourceType != null
                                                    && !excludedPropagatedIds
                                                            .contains(relationshipColumn
                                                                    .getId())) {
                                                propagatedRelationshipColumns
                                                        .add(new PropagatedRelationshipColumn(
                                                                savedRelationshipColumn
                                                                        .getId(),
                                                                relationshipId,
                                                                fkColumnId,
                                                                refColumnId,
                                                                entity
                                                                        .getSeqNo(),
                                                                sourceType,
                                                                sourceId));
                                            }
                                        })
                                        .then();
                            }))
                    .then();

            return saveRelationshipColumnsTask.then(Mono.fromSupplier(() -> {
                PropagatedEntities propagated = new PropagatedEntities(
                        propagatedColumns,
                        propagatedRelationshipColumns,
                        propagatedConstraints,
                        propagatedConstraintColumns,
                        propagatedIndexColumns);
                IdMappings idMappings = new IdMappings(
                        Map.copyOf(schemaIdMap),
                        Map.copyOf(tableIdMap),
                        Map.copyOf(columnIdMap),
                        Map.copyOf(indexIdMap),
                        Map.copyOf(indexColumnIdMap),
                        Map.copyOf(constraintIdMap),
                        Map.copyOf(constraintColumnIdMap),
                        Map.copyOf(relationshipIdMap),
                        Map.copyOf(relationshipColumnIdMap));
                return new SaveResult(propagated, idMappings);
            }));
        });
    }

    private static boolean shouldSaveEntity(
            String entityId,
            boolean isAffected,
            Set<String> beforeIds,
            Set<String> excludedIds) {
        return isAffected && !beforeIds.contains(entityId)
                && !excludedIds.contains(entityId);
    }

    private static String remapId(String originalId, Map<String, String> idMap) {
        String mappedId = idMap.get(originalId);
        return mappedId != null ? mappedId : originalId;
    }

    private Set<String> collectAllEntityIds(
            Validation.Database database) {
        Set<String> ids = new HashSet<>();

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

    private record RelationshipColumnWithRelationshipId(
            Validation.RelationshipColumn relationshipColumn,
            String relationshipId) {
    }

    private record ConstraintColumnWithConstraintId(
            Validation.ConstraintColumn constraintColumn,
            String constraintId) {
    }

    private record SavedPropagatedColumn(String columnId, String tableId) {
    }

    public record SaveResult(
            PropagatedEntities propagated,
            IdMappings idMappings) {
    }

    private static Map<String, String> buildFkToRefColumnIdMap(
            Validation.Database database,
            String sourceType,
            String sourceId,
            Map<String, String> columnIdMap) {
        Map<String, String> fkToRefMap = new HashMap<>();

        if (EntityType.RELATIONSHIP.name().equals(sourceType)) {
            Validation.Relationship relationship = findRelationshipById(database,
                    sourceId);
            if (relationship == null) {
                return fkToRefMap;
            }

            for (Validation.RelationshipColumn relationshipColumn : relationship
                    .getColumnsList()) {
                if (relationshipColumn.getFkColumnId().isBlank()) {
                    continue;
                }

                fkToRefMap.put(
                        remapId(relationshipColumn.getFkColumnId(), columnIdMap),
                        remapId(relationshipColumn.getRefColumnId(),
                                columnIdMap));
            }
            return fkToRefMap;
        }

        if (EntityType.CONSTRAINT.name().equals(sourceType)) {
            Validation.Constraint constraint = findConstraintById(database,
                    sourceId);
            if (constraint == null) {
                return fkToRefMap;
            }

            Set<String> parentColumnIds = constraint.getColumnsList().stream()
                    .map(Validation.ConstraintColumn::getColumnId)
                    .collect(Collectors.toSet());

            for (Validation.Schema schema : database.getSchemasList()) {
                for (Validation.Table table : schema.getTablesList()) {
                    for (Validation.Relationship relationship : table
                            .getRelationshipsList()) {
                        for (Validation.RelationshipColumn relationshipColumn : relationship
                                .getColumnsList()) {
                            if (!parentColumnIds.contains(
                                    relationshipColumn.getRefColumnId())) {
                                continue;
                            }

                            if (relationshipColumn.getFkColumnId().isBlank()) {
                                continue;
                            }

                            fkToRefMap.put(remapId(
                                    relationshipColumn.getFkColumnId(),
                                    columnIdMap), remapId(
                                            relationshipColumn.getRefColumnId(),
                                            columnIdMap));
                        }
                    }
                }
            }
        }

        return fkToRefMap;
    }

    private static Validation.Relationship findRelationshipById(
            Validation.Database database,
            String relationshipId) {
        for (Validation.Schema schema : database.getSchemasList()) {
            for (Validation.Table table : schema.getTablesList()) {
                for (Validation.Relationship relationship : table
                        .getRelationshipsList()) {
                    if (relationship.getId().equals(relationshipId)) {
                        return relationship;
                    }
                }
            }
        }
        return null;
    }

    private static Validation.Constraint findConstraintById(
            Validation.Database database,
            String constraintId) {
        for (Validation.Schema schema : database.getSchemasList()) {
            for (Validation.Table table : schema.getTablesList()) {
                for (Validation.Constraint constraint : table
                        .getConstraintsList()) {
                    if (constraint.getId().equals(constraintId)) {
                        return constraint;
                    }
                }
            }
        }
        return null;
    }

}
