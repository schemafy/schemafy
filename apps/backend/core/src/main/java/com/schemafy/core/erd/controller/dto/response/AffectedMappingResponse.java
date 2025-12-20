package com.schemafy.core.erd.controller.dto.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.schemafy.core.erd.model.EntityType;

import validation.Validation;

public record AffectedMappingResponse(
        Map<String, String> schemas,
        Map<String, String> tables,
        Map<String, Map<String, String>> columns,
        Map<String, Map<String, String>> indexes,
        Map<String, Map<String, String>> indexColumns,
        Map<String, Map<String, String>> constraints,
        Map<String, Map<String, String>> constraintColumns,
        Map<String, Map<String, String>> relationships,
        Map<String, Map<String, String>> relationshipColumns,
        PropagatedEntities propagated) {

    public record PropagatedEntities(
            List<PropagatedColumn> columns,
            List<PropagatedRelationshipColumn> relationshipColumns,
            List<PropagatedConstraintColumn> constraintColumns,
            List<PropagatedIndexColumn> indexColumns) {

        public static PropagatedEntities empty() {
            return new PropagatedEntities(List.of(), List.of(), List.of(),
                    List.of());
        }

    }

    public record PropagatedColumn(
            String columnId,
            String tableId,
            String sourceType,
            String sourceId,
            String sourceColumnId) {
    }

    public record PropagatedRelationshipColumn(
            String relationshipColumnId,
            String relationshipId,
            String fkColumnId,
            String refColumnId,
            int seqNo,
            String sourceType,
            String sourceId) {
    }

    public record PropagatedConstraintColumn(
            String constraintColumnId,
            String constraintId,
            String columnId,
            String sourceType,
            String sourceId) {
    }

    public record PropagatedIndexColumn(
            String indexColumnId,
            String indexId,
            String columnId,
            String sourceType,
            String sourceId) {
    }

    public static AffectedMappingResponse of(
            Validation.CreateSchemaRequest request, Validation.Database before,
            Validation.Database after) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasSchema()) {
            extractSchemaIds(request.getSchema(), requestIdMaps);
        }
        return buildResponse(before, after, requestIdMaps,
                PropagatedEntities.empty());
    }

    public static AffectedMappingResponse of(
            Validation.CreateTableRequest request, Validation.Database before,
            Validation.Database after) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasTable()) {
            extractTableIds(request.getTable(), requestIdMaps);
        }
        return buildResponse(before, after, requestIdMaps,
                PropagatedEntities.empty());
    }

    public static AffectedMappingResponse of(
            Validation.CreateColumnRequest request, Validation.Database before,
            Validation.Database after) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasColumn()) {
            requestIdMaps.columns.put(
                    request.getColumn().getName(),
                    request.getColumn().getId());
        }
        return buildResponse(before, after, requestIdMaps,
                PropagatedEntities.empty());
    }

    public static AffectedMappingResponse of(
            Validation.CreateConstraintRequest request,
            Validation.Database before,
            Validation.Database after,
            PropagatedEntities propagated) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasConstraint()) {
            extractConstraintIds(request.getConstraint(), requestIdMaps);
        }
        return buildResponse(before, after, requestIdMaps, propagated);
    }

    public static AffectedMappingResponse of(
            Validation.CreateIndexRequest request, Validation.Database before,
            Validation.Database after) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasIndex()) {
            extractIndexIds(request.getIndex(), requestIdMaps);
        }
        return buildResponse(before, after, requestIdMaps,
                PropagatedEntities.empty());
    }

    public static AffectedMappingResponse of(
            Validation.CreateRelationshipRequest request,
            Validation.Database before,
            Validation.Database after,
            PropagatedEntities propagated) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasRelationship()) {
            extractRelationshipIds(request.getRelationship(), requestIdMaps);
        }
        return buildResponse(before, after, requestIdMaps, propagated);
    }

    public static AffectedMappingResponse of(
            Validation.AddColumnToConstraintRequest request,
            Validation.Database before,
            Validation.Database after,
            PropagatedEntities propagated) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasConstraintColumn()) {
            requestIdMaps.constraintColumns.put(
                    request.getConstraintColumn().getColumnId(),
                    request.getConstraintColumn().getId());
        }
        return buildResponse(before, after, requestIdMaps, propagated);
    }

    public static AffectedMappingResponse of(
            Validation.AddColumnToRelationshipRequest request,
            Validation.Database before,
            Validation.Database after,
            PropagatedEntities propagated) {
        RequestIdMaps requestIdMaps = new RequestIdMaps();
        if (request.hasRelationshipColumn()) {
            String businessKey = request.getRelationshipColumn()
                    .getFkColumnId() + ":"
                    + request.getRelationshipColumn().getRefColumnId();
            requestIdMaps.relationshipColumns.put(
                    businessKey,
                    request.getRelationshipColumn().getId());
        }
        return buildResponse(before, after, requestIdMaps, propagated);
    }

    private static AffectedMappingResponse buildResponse(
            Validation.Database before, Validation.Database after,
            RequestIdMaps requestIdMaps, PropagatedEntities propagated) {
        AffectedMappingResponseBuilder builder = new AffectedMappingResponseBuilder();
        builder.setPropagated(propagated);
        mapSchemasWithMaps(builder, before, after, requestIdMaps);
        return builder.build();
    }

    private static void extractSchemaIds(Validation.Schema schema,
            RequestIdMaps maps) {
        maps.schemas.put(schema.getName(), schema.getId());
        for (Validation.Table table : schema.getTablesList()) {
            extractTableIds(table, maps);
        }
    }

    private static void extractTableIds(Validation.Table table,
            RequestIdMaps maps) {
        maps.tables.put(table.getName(), table.getId());

        for (Validation.Column column : table.getColumnsList()) {
            maps.columns.put(column.getName(), column.getId());
        }

        for (Validation.Index index : table.getIndexesList()) {
            extractIndexIds(index, maps);
        }

        for (Validation.Constraint constraint : table.getConstraintsList()) {
            extractConstraintIds(constraint, maps);
        }

        for (Validation.Relationship relationship : table
                .getRelationshipsList()) {
            extractRelationshipIds(relationship, maps);
        }
    }

    private static void extractIndexIds(Validation.Index index,
            RequestIdMaps maps) {
        maps.indexes.put(index.getName(), index.getId());
        for (Validation.IndexColumn indexColumn : index.getColumnsList()) {
            maps.indexColumns.put(indexColumn.getColumnId(),
                    indexColumn.getId());
        }
    }

    private static void extractConstraintIds(Validation.Constraint constraint,
            RequestIdMaps maps) {
        maps.constraints.put(constraint.getName(), constraint.getId());
        for (Validation.ConstraintColumn constraintColumn : constraint
                .getColumnsList()) {
            maps.constraintColumns.put(constraintColumn.getColumnId(),
                    constraintColumn.getId());
        }
    }

    private static void extractRelationshipIds(
            Validation.Relationship relationship, RequestIdMaps maps) {
        maps.relationships.put(relationship.getName(), relationship.getId());
        for (Validation.RelationshipColumn relationshipColumn : relationship
                .getColumnsList()) {
            String businessKey = relationshipColumn.getFkColumnId() + ":"
                    + relationshipColumn.getRefColumnId();
            maps.relationshipColumns.put(businessKey,
                    relationshipColumn.getId());
        }
    }

    public static Validation.Database updateEntityIdInDatabase(
            Validation.Database database,
            EntityType entityType,
            String feId,
            String beId) {
        return switch (entityType) {
        case SCHEMA -> updateSchemaId(database, feId, beId);
        case TABLE -> updateTableId(database, feId, beId);
        case COLUMN -> updateColumnId(database, feId, beId);
        case INDEX -> updateIndexId(database, feId, beId);
        case CONSTRAINT -> updateConstraintId(database, feId, beId);
        case CONSTRAINT_COLUMN -> updateConstraintColumnId(database, feId,
                beId);
        case RELATIONSHIP -> updateRelationshipId(database, feId, beId);
        case RELATIONSHIP_COLUMN -> updateRelationshipColumnId(database, feId,
                beId);
        };
    }

    private static Validation.Database updateSchemaId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            if (schema.getId().equals(feId)) {
                dbBuilder.addSchemas(schema.toBuilder().setId(beId).build());
            } else {
                dbBuilder.addSchemas(schema);
            }
        }
        return dbBuilder.build();
    }

    private static Validation.Database updateTableId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                if (table.getId().equals(feId)) {
                    Validation.Table.Builder tableBuilder = table.toBuilder()
                            .setId(beId);

                    tableBuilder.clearColumns();
                    for (Validation.Column column : table.getColumnsList()) {
                        tableBuilder.addColumns(column.toBuilder()
                                .setTableId(beId).build());
                    }

                    tableBuilder.clearIndexes();
                    for (Validation.Index index : table.getIndexesList()) {
                        tableBuilder.addIndexes(index.toBuilder()
                                .setTableId(beId).build());
                    }

                    tableBuilder.clearConstraints();
                    for (Validation.Constraint constraint : table
                            .getConstraintsList()) {
                        tableBuilder.addConstraints(constraint.toBuilder()
                                .setTableId(beId).build());
                    }

                    tableBuilder.clearRelationships();
                    for (Validation.Relationship relationship : table
                            .getRelationshipsList()) {
                        Validation.Relationship.Builder relBuilder = relationship
                                .toBuilder();
                        if (relationship.getSrcTableId().equals(feId)) {
                            relBuilder.setSrcTableId(beId);
                        }
                        if (relationship.getTgtTableId().equals(feId)) {
                            relBuilder.setTgtTableId(beId);
                        }
                        tableBuilder.addRelationships(relBuilder.build());
                    }

                    schemaBuilder.addTables(tableBuilder.build());
                } else {
                    schemaBuilder.addTables(table);
                }
            }
            dbBuilder.addSchemas(schemaBuilder.build());
        }
        return dbBuilder.build();
    }

    private static Validation.Database updateColumnId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                Validation.Table.Builder tableBuilder = table.toBuilder();
                tableBuilder.clearColumns();

                for (Validation.Column column : table.getColumnsList()) {
                    if (column.getId().equals(feId)) {
                        tableBuilder.addColumns(column.toBuilder()
                                .setId(beId).build());
                    } else {
                        tableBuilder.addColumns(column);
                    }
                }
                schemaBuilder.addTables(tableBuilder.build());
            }
            dbBuilder.addSchemas(schemaBuilder.build());
        }
        return dbBuilder.build();
    }

    private static Validation.Database updateIndexId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                Validation.Table.Builder tableBuilder = table.toBuilder();
                tableBuilder.clearIndexes();

                for (Validation.Index index : table.getIndexesList()) {
                    if (index.getId().equals(feId)) {
                        tableBuilder.addIndexes(index.toBuilder()
                                .setId(beId).build());
                    } else {
                        tableBuilder.addIndexes(index);
                    }
                }
                schemaBuilder.addTables(tableBuilder.build());
            }
            dbBuilder.addSchemas(schemaBuilder.build());
        }
        return dbBuilder.build();
    }

    private static Validation.Database updateConstraintId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                Validation.Table.Builder tableBuilder = table.toBuilder();
                tableBuilder.clearConstraints();

                for (Validation.Constraint constraint : table
                        .getConstraintsList()) {
                    if (constraint.getId().equals(feId)) {
                        tableBuilder.addConstraints(constraint.toBuilder()
                                .setId(beId).build());
                    } else {
                        tableBuilder.addConstraints(constraint);
                    }
                }
                schemaBuilder.addTables(tableBuilder.build());
            }
            dbBuilder.addSchemas(schemaBuilder.build());
        }
        return dbBuilder.build();
    }

    private static Validation.Database updateRelationshipId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                Validation.Table.Builder tableBuilder = table.toBuilder();
                tableBuilder.clearRelationships();

                for (Validation.Relationship relationship : table
                        .getRelationshipsList()) {
                    if (relationship.getId().equals(feId)) {
                        tableBuilder.addRelationships(relationship.toBuilder()
                                .setId(beId).build());
                    } else {
                        tableBuilder.addRelationships(relationship);
                    }
                }
                schemaBuilder.addTables(tableBuilder.build());
            }
            dbBuilder.addSchemas(schemaBuilder.build());
        }
        return dbBuilder.build();
    }

    private static Validation.Database updateConstraintColumnId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                Validation.Table.Builder tableBuilder = table.toBuilder();
                tableBuilder.clearConstraints();

                for (Validation.Constraint constraint : table
                        .getConstraintsList()) {
                    Validation.Constraint.Builder constraintBuilder = constraint
                            .toBuilder();
                    constraintBuilder.clearColumns();

                    for (Validation.ConstraintColumn constraintColumn : constraint
                            .getColumnsList()) {
                        if (constraintColumn.getId().equals(feId)) {
                            constraintBuilder.addColumns(constraintColumn
                                    .toBuilder()
                                    .setId(beId)
                                    .build());
                        } else {
                            constraintBuilder.addColumns(constraintColumn);
                        }
                    }
                    tableBuilder.addConstraints(constraintBuilder.build());
                }
                schemaBuilder.addTables(tableBuilder.build());
            }
            dbBuilder.addSchemas(schemaBuilder.build());
        }
        return dbBuilder.build();
    }

    private static Validation.Database updateRelationshipColumnId(
            Validation.Database database, String feId, String beId) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                Validation.Table.Builder tableBuilder = table.toBuilder();
                tableBuilder.clearRelationships();

                for (Validation.Relationship relationship : table
                        .getRelationshipsList()) {
                    Validation.Relationship.Builder relationshipBuilder = relationship
                            .toBuilder();
                    relationshipBuilder.clearColumns();

                    for (Validation.RelationshipColumn relationshipColumn : relationship
                            .getColumnsList()) {
                        if (relationshipColumn.getId().equals(feId)) {
                            relationshipBuilder
                                    .addColumns(relationshipColumn.toBuilder()
                                            .setId(beId)
                                            .build());
                        } else {
                            relationshipBuilder.addColumns(relationshipColumn);
                        }
                    }
                    tableBuilder.addRelationships(relationshipBuilder.build());
                }
                schemaBuilder.addTables(tableBuilder.build());
            }
            dbBuilder.addSchemas(schemaBuilder.build());
        }
        return dbBuilder.build();
    }

    private static class RequestIdMaps {

        final Map<String, String> schemas = new HashMap<>();
        final Map<String, String> tables = new HashMap<>();
        final Map<String, String> columns = new HashMap<>();
        final Map<String, String> indexes = new HashMap<>();
        final Map<String, String> indexColumns = new HashMap<>();
        final Map<String, String> constraints = new HashMap<>();
        final Map<String, String> constraintColumns = new HashMap<>();
        final Map<String, String> relationships = new HashMap<>();
        final Map<String, String> relationshipColumns = new HashMap<>();

    }

    private static <T> void mapEntities(
            List<T> beforeList,
            List<T> afterList,
            Function<T, String> idExtractor,
            Function<T, String> businessKeyExtractor,
            Function<T, Boolean> isAffectedExtractor,
            BiConsumer<String, String> idMappingConsumer,
            BiConsumer<T, T> recursiveMapper,
            Map<String, String> requestIdMap) {

        Map<String, T> beforeMapById = beforeList.stream()
                .collect(Collectors.toMap(idExtractor, Function.identity(),
                        (k1, k2) -> k1));

        List<T> newAfterEntities = new ArrayList<>();

        for (T afterEntity : afterList) {
            T beforeEntity = beforeMapById
                    .remove(idExtractor.apply(afterEntity));
            if (beforeEntity != null) {
                recursiveMapper.accept(beforeEntity, afterEntity);
            } else {
                newAfterEntities.add(afterEntity);
            }
        }

        if (!beforeMapById.isEmpty() && !newAfterEntities.isEmpty()) {
            Map<String, T> tempFeMapByKey = beforeMapById.values().stream()
                    .collect(Collectors.toMap(businessKeyExtractor,
                            Function.identity(), (k1, k2) -> k1));

            for (T newAfterEntity : newAfterEntities) {
                T tempFeEntity = tempFeMapByKey
                        .get(businessKeyExtractor.apply(newAfterEntity));
                if (tempFeEntity != null) {
                    idMappingConsumer.accept(
                            idExtractor.apply(tempFeEntity),
                            idExtractor.apply(newAfterEntity));
                    if (Boolean.TRUE.equals(
                            isAffectedExtractor.apply(newAfterEntity))) {
                        recursiveMapper.accept(tempFeEntity, newAfterEntity);
                    }
                }
            }
        }

        if (!newAfterEntities.isEmpty()) {
            for (T newAfterEntity : newAfterEntities) {
                String afterId = idExtractor.apply(newAfterEntity);
                String businessKey = businessKeyExtractor
                        .apply(newAfterEntity);

                String feId = requestIdMap.get(businessKey);
                if (feId != null) {
                    idMappingConsumer.accept(feId, afterId);
                    recursiveMapper.accept(null, newAfterEntity);
                } else if (Boolean.TRUE
                        .equals(isAffectedExtractor.apply(newAfterEntity))) {
                    recursiveMapper.accept(null, newAfterEntity);
                }
            }
        }
    }

    private static void mapSchemasWithMaps(
            AffectedMappingResponseBuilder builder, Validation.Database before,
            Validation.Database after, RequestIdMaps requestIdMaps) {
        mapEntities(
                before.getSchemasList(),
                after.getSchemasList(),
                Validation.Schema::getId,
                Validation.Schema::getName,
                Validation.Schema::getIsAffected,
                (feId, beId) -> builder.schemas.put(feId, beId),
                (beforeSchema, afterSchema) -> {
                    Validation.Schema beforeSchemaSafe = beforeSchema != null
                            ? beforeSchema
                            : Validation.Schema.getDefaultInstance();
                    mapTablesWithMaps(builder, beforeSchemaSafe, afterSchema,
                            requestIdMaps);
                },
                requestIdMaps.schemas);
    }

    private static void mapTablesWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Schema beforeSchema,
            Validation.Schema afterSchema,
            RequestIdMaps requestIdMaps) {
        mapEntities(
                beforeSchema.getTablesList(),
                afterSchema.getTablesList(),
                Validation.Table::getId,
                Validation.Table::getName,
                Validation.Table::getIsAffected,
                (feId, beId) -> builder.tables.put(feId, beId),
                (beforeTable, afterTable) -> {
                    Validation.Table beforeTableSafe = beforeTable != null
                            ? beforeTable
                            : Validation.Table.getDefaultInstance();
                    mapColumnsWithMaps(builder, beforeTableSafe, afterTable,
                            requestIdMaps);
                    mapIndexesWithMaps(builder, beforeTableSafe, afterTable,
                            requestIdMaps);
                    mapConstraintsWithMaps(builder, beforeTableSafe, afterTable,
                            requestIdMaps);
                    mapRelationshipsWithMaps(builder, beforeTableSafe,
                            afterTable,
                            requestIdMaps);
                },
                requestIdMaps.tables);
    }

    private static void mapColumnsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Table beforeTable,
            Validation.Table afterTable,
            RequestIdMaps requestIdMaps) {
        mapEntities(
                beforeTable.getColumnsList(),
                afterTable.getColumnsList(),
                Validation.Column::getId,
                Validation.Column::getName,
                Validation.Column::getIsAffected,
                (feId, beId) -> builder.columns
                        .computeIfAbsent(afterTable.getId(),
                                k -> new HashMap<>())
                        .put(feId, beId),
                (beforeColumn, afterColumn) -> {
                },
                requestIdMaps.columns);
    }

    private static void mapIndexesWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Table beforeTable,
            Validation.Table afterTable,
            RequestIdMaps requestIdMaps) {
        mapEntities(
                beforeTable.getIndexesList(),
                afterTable.getIndexesList(),
                Validation.Index::getId,
                Validation.Index::getName,
                Validation.Index::getIsAffected,
                (feId, beId) -> builder.indexes
                        .computeIfAbsent(afterTable.getId(),
                                k -> new HashMap<>())
                        .put(feId, beId),
                (beforeIndex, afterIndex) -> mapIndexColumnsWithMaps(builder,
                        beforeIndex, afterIndex, requestIdMaps),
                requestIdMaps.indexes);
    }

    private static void mapIndexColumnsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Index beforeIndex,
            Validation.Index afterIndex,
            RequestIdMaps requestIdMaps) {
        Validation.Index beforeIndexSafe = beforeIndex != null
                ? beforeIndex
                : Validation.Index.getDefaultInstance();

        mapEntities(
                beforeIndexSafe.getColumnsList(),
                afterIndex.getColumnsList(),
                Validation.IndexColumn::getId,
                Validation.IndexColumn::getColumnId,
                Validation.IndexColumn::getIsAffected,
                (feId, beId) -> builder.indexColumns
                        .computeIfAbsent(afterIndex.getId(),
                                k -> new HashMap<>())
                        .put(feId, beId),
                (beforeIndexColumn, afterIndexColumn) -> {
                },
                requestIdMaps.indexColumns);
    }

    private static void mapConstraintsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Table beforeTable,
            Validation.Table afterTable,
            RequestIdMaps requestIdMaps) {
        mapEntities(
                beforeTable.getConstraintsList(),
                afterTable.getConstraintsList(),
                Validation.Constraint::getId,
                Validation.Constraint::getName,
                Validation.Constraint::getIsAffected,
                (feId, beId) -> builder.constraints
                        .computeIfAbsent(afterTable.getId(),
                                k -> new HashMap<>())
                        .put(feId, beId),
                (beforeConstraint,
                        afterConstraint) -> mapConstraintColumnsWithMaps(
                                builder, beforeConstraint, afterConstraint,
                                requestIdMaps),
                requestIdMaps.constraints);
    }

    private static void mapConstraintColumnsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Constraint beforeConstraint,
            Validation.Constraint afterConstraint,
            RequestIdMaps requestIdMaps) {
        Validation.Constraint beforeConstraintSafe = beforeConstraint != null
                ? beforeConstraint
                : Validation.Constraint.getDefaultInstance();

        mapEntities(
                beforeConstraintSafe.getColumnsList(),
                afterConstraint.getColumnsList(),
                Validation.ConstraintColumn::getId,
                Validation.ConstraintColumn::getColumnId,
                Validation.ConstraintColumn::getIsAffected,
                (feId, beId) -> builder.constraintColumns
                        .computeIfAbsent(afterConstraint.getId(),
                                k -> new HashMap<>())
                        .put(feId, beId),
                (beforeConstraintColumn, afterConstraintColumn) -> {
                },
                requestIdMaps.constraintColumns);
    }

    private static void mapRelationshipsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Table beforeTable,
            Validation.Table afterTable,
            RequestIdMaps requestIdMaps) {
        mapEntities(
                beforeTable.getRelationshipsList(),
                afterTable.getRelationshipsList(),
                Validation.Relationship::getId,
                Validation.Relationship::getName,
                Validation.Relationship::getIsAffected,
                (feId, beId) -> builder.relationships
                        .computeIfAbsent(afterTable.getId(),
                                k -> new HashMap<>())
                        .put(feId, beId),
                (beforeRelationship,
                        afterRelationship) -> mapRelationshipColumnsWithMaps(
                                builder,
                                beforeRelationship, afterRelationship,
                                requestIdMaps),
                requestIdMaps.relationships);
    }

    private static void mapRelationshipColumnsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Relationship beforeRelationship,
            Validation.Relationship afterRelationship,
            RequestIdMaps requestIdMaps) {
        Validation.Relationship beforeRelationshipSafe = beforeRelationship != null
                ? beforeRelationship
                : Validation.Relationship.getDefaultInstance();

        mapEntities(
                beforeRelationshipSafe.getColumnsList(),
                afterRelationship.getColumnsList(),
                Validation.RelationshipColumn::getId,
                c -> c.getFkColumnId() + ":" + c.getRefColumnId(),
                Validation.RelationshipColumn::getIsAffected,
                (feId, beId) -> builder.relationshipColumns
                        .computeIfAbsent(afterRelationship.getId(),
                                k -> new HashMap<>())
                        .put(feId, beId),
                (beforeRelationshipColumn, afterRelationshipColumn) -> {
                },
                requestIdMaps.relationshipColumns);
    }

    private static class AffectedMappingResponseBuilder {

        private final Map<String, String> schemas = new HashMap<>();
        private final Map<String, String> tables = new HashMap<>();
        private final Map<String, Map<String, String>> columns = new HashMap<>();
        private final Map<String, Map<String, String>> indexes = new HashMap<>();
        private final Map<String, Map<String, String>> indexColumns = new HashMap<>();
        private final Map<String, Map<String, String>> constraints = new HashMap<>();
        private final Map<String, Map<String, String>> constraintColumns = new HashMap<>();
        private final Map<String, Map<String, String>> relationships = new HashMap<>();
        private final Map<String, Map<String, String>> relationshipColumns = new HashMap<>();
        private PropagatedEntities propagated = PropagatedEntities.empty();

        public void setPropagated(PropagatedEntities propagated) {
            this.propagated = propagated;
        }

        public AffectedMappingResponse build() {
            return new AffectedMappingResponse(
                    schemas,
                    tables,
                    columns,
                    indexes,
                    indexColumns,
                    constraints,
                    constraintColumns,
                    relationships,
                    relationshipColumns,
                    propagated);
        }

    }

}
