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

/**
 * Mapping result between frontend IDs (FE-ID) and backend IDs (BE-ID) with one-level hierarchical grouping.
 *
 * <p>General rules
 * <ul>
 *   <li>Flat maps (schemas, tables): {@code FE-ID -> BE-ID}</li>
 *   <li>Nested maps (others): {@code Parent-BE-ID -> ( FE-ID -> BE-ID )}</li>
 *   <li>"Parent-BE-ID" is the immediate upper-level entity ID for the group (e.g., tableId for columns).</li>
 * </ul>
 *
 * <p>Grouping basis
 * <ul>
 *   <li>columns: grouped by tableId</li>
 *   <li>indexes: grouped by tableId</li>
 *   <li>indexColumns: grouped by indexId</li>
 *   <li>constraints: grouped by tableId</li>
 *   <li>constraintColumns: grouped by constraintId</li>
 *   <li>relationships: grouped by tableId (owner table)</li>
 *   <li>relationshipColumns: grouped by relationshipId</li>
 * </ul>
 *
 * <p>Example
 * <pre>
 * {
 *   schemas:  { fe-schema-1: be-schema-1 },
 *   tables:   { fe-table-1:  be-table-1  },
 *   columns:  { be-table-1:  { fe-col-1:  be-col-1 } },
 *   indexes:  { be-table-1:  { fe-idx-1:  be-idx-1 } },
 *   indexColumns: { be-idx-1: { fe-idxcol-1: be-idxcol-1 } },
 *   constraints: { be-table-1: { fe-const-1: be-const-1 } },
 *   constraintColumns: { be-const-1: { fe-cc-1: be-cc-1 } },
 *   relationships: { be-table-1: { fe-rel-1: be-rel-1 } },
 *   relationshipColumns: { be-rel-1: { fe-rc-1: be-rc-1 } }
 * }
 * </pre>
 */
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

    /**
     * 전파로 생성된 엔티티들과 그 출처 정보
     */
    public record PropagatedEntities(
            List<PropagatedColumn> columns,
            List<PropagatedConstraintColumn> constraintColumns,
            List<PropagatedIndexColumn> indexColumns) {

        public static PropagatedEntities empty() {
            return new PropagatedEntities(List.of(), List.of(), List.of());
        }

    }

    /**
     * 전파된 컬럼 정보
     *
     * @param columnId 생성된 컬럼의 BE-ID
     * @param tableId 컬럼이 추가된 테이블의 BE-ID
     * @param sourceType 전파 출처 타입 ("RELATIONSHIP" 또는 "CONSTRAINT")
     * @param sourceId 출처 엔티티의 BE-ID (Relationship 또는 Constraint)
     * @param sourceColumnId 전파된 원본 컬럼의 BE-ID
     */
    public record PropagatedColumn(
            String columnId,
            String tableId,
            String sourceType,
            String sourceId,
            String sourceColumnId) {
    }

    /**
     * 전파된 제약조건 컬럼 정보
     *
     * @param constraintColumnId 생성된 제약조건 컬럼의 BE-ID
     * @param constraintId 제약조건 컬럼이 추가된 제약조건의 BE-ID
     * @param columnId 추가된 컬럼의 BE-ID (위에서 전파된 컬럼)
     * @param sourceType 전파 출처 타입 ("RELATIONSHIP" 또는 "CONSTRAINT")
     * @param sourceId 출처 엔티티의 BE-ID
     */
    public record PropagatedConstraintColumn(
            String constraintColumnId,
            String constraintId,
            String columnId,
            String sourceType,
            String sourceId) {
    }

    /**
     * 전파된 인덱스 컬럼 정보
     *
     * @param indexColumnId 생성된 인덱스 컬럼의 BE-ID
     * @param indexId 인덱스 컬럼이 추가된 인덱스의 BE-ID
     * @param columnId 추가된 컬럼의 BE-ID (위에서 전파된 컬럼)
     * @param sourceType 전파 출처 타입 ("RELATIONSHIP" 또는 "CONSTRAINT")
     * @param sourceId 출처 엔티티의 BE-ID
     */
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

    /**
     * 공통 응답 빌드 로직
     */
    private static AffectedMappingResponse buildResponse(
            Validation.Database before, Validation.Database after,
            RequestIdMaps requestIdMaps, PropagatedEntities propagated) {
        AffectedMappingResponseBuilder builder = new AffectedMappingResponseBuilder();
        builder.setPropagated(propagated);
        mapSchemasWithMaps(builder, before, after, requestIdMaps);
        return builder.build();
    }

    /**
     * 스키마에서 모든 하위 엔티티의 ID를 추출
     */
    private static void extractSchemaIds(Validation.Schema schema,
            RequestIdMaps maps) {
        maps.schemas.put(schema.getName(), schema.getId());
        for (Validation.Table table : schema.getTablesList()) {
            extractTableIds(table, maps);
        }
    }

    /**
     * 테이블에서 모든 하위 엔티티의 ID를 추출
     */
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

    /**
     * 인덱스와 인덱스 컬럼의 ID를 추출
     */
    private static void extractIndexIds(Validation.Index index,
            RequestIdMaps maps) {
        maps.indexes.put(index.getName(), index.getId());
        for (Validation.IndexColumn indexColumn : index.getColumnsList()) {
            maps.indexColumns.put(indexColumn.getColumnId(),
                    indexColumn.getId());
        }
    }

    /**
     * 제약조건과 제약조건 컬럼의 ID를 추출
     */
    private static void extractConstraintIds(Validation.Constraint constraint,
            RequestIdMaps maps) {
        maps.constraints.put(constraint.getName(), constraint.getId());
        for (Validation.ConstraintColumn constraintColumn : constraint
                .getColumnsList()) {
            maps.constraintColumns.put(constraintColumn.getColumnId(),
                    constraintColumn.getId());
        }
    }

    /**
     * 관계와 관계 컬럼의 ID를 추출
     */
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

    /**
     * TODO: 향후 다른 요청 타입들(Update, Delete 등)에 대해서도 동일한 패턴으로 구현 필요
     */

    /**
     * Validation 결과의 Database에서 특정 엔티티 ID를 실제 DB ID로 교체
     * 
     * @param database Validation 결과 Database
     * @param entityType 엔티티 타입
     * @param feId 프론트엔드에서 보낸 임시 ID
     * @param beId DB에서 생성된 실제 ID
     * @return 업데이트된 Database
     */
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
        case RELATIONSHIP -> updateRelationshipId(database, feId, beId);
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
                    // 테이블 ID를 실제 DB ID로 변경하고 하위 엔티티도 업데이트
                    Validation.Table.Builder tableBuilder = table.toBuilder()
                            .setId(beId);

                    // Columns의 tableId 업데이트
                    tableBuilder.clearColumns();
                    for (Validation.Column column : table.getColumnsList()) {
                        tableBuilder.addColumns(column.toBuilder()
                                .setTableId(beId).build());
                    }

                    // Indexes의 tableId 업데이트
                    tableBuilder.clearIndexes();
                    for (Validation.Index index : table.getIndexesList()) {
                        tableBuilder.addIndexes(index.toBuilder()
                                .setTableId(beId).build());
                    }

                    // Constraints의 tableId 업데이트
                    tableBuilder.clearConstraints();
                    for (Validation.Constraint constraint : table
                            .getConstraintsList()) {
                        tableBuilder.addConstraints(constraint.toBuilder()
                                .setTableId(beId).build());
                    }

                    // Relationships의 srcTableId/tgtTableId 업데이트
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

    /**
     * 요청 객체에서 추출한 프론트엔드 ID를 엔티티 타입별로 관리하는 컨테이너
     */
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

        // 기존 엔티티와 새 엔티티 매핑 (beforeMapById가 비어있지 않은 경우)
        // 비즈니스 키(이름 등)로 매칭하여 기존 엔티티의 FE-ID와 새 엔티티의 BE-ID를 매핑
        // isAffected가 없거나 false인 경우도 false와 같이 처리되므로 체크 불필요
        if (!beforeMapById.isEmpty() && !newAfterEntities.isEmpty()) {
            Map<String, T> tempFeMapByKey = beforeMapById.values().stream()
                    .collect(Collectors.toMap(businessKeyExtractor,
                            Function.identity(), (k1, k2) -> k1));

            for (T newAfterEntity : newAfterEntities) {
                T tempFeEntity = tempFeMapByKey
                        .get(businessKeyExtractor.apply(newAfterEntity));
                if (tempFeEntity != null) {
                    // isAffected가 true인 경우에만 하위 엔티티 재귀 매핑 수행
                    // 하지만 ID 매핑은 항상 수행 (isAffected와 무관)
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

        // 새로 생성된 엔티티들을 요청 ID 맵과 매핑
        // newAfterEntities는 before에 없고 after에만 있는 엔티티들
        // requestIdMap에 있는 엔티티는 요청에 포함된 것이므로 isAffected와 관계없이 매핑
        // requestIdMap에 없고 isAffected가 true인 엔티티는 전파로 생성된 것이므로 매핑 (하위 엔티티만)
        if (!newAfterEntities.isEmpty()) {
            for (T newAfterEntity : newAfterEntities) {
                String afterId = idExtractor.apply(newAfterEntity);
                String businessKey = businessKeyExtractor
                        .apply(newAfterEntity);

                // 요청 ID 맵에서 비즈니스 키로 매칭되는 FE-ID를 찾아서 매핑
                String feId = requestIdMap.get(businessKey);
                if (feId != null) {
                    // 요청에 포함된 엔티티이므로 무조건 매핑
                    idMappingConsumer.accept(feId, afterId);
                    // 하위 엔티티도 재귀적으로 매핑
                    // 새로 생성된 엔티티의 경우 beforeEntity가 null이므로,
                    // recursiveMapper를 호출할 때 null을 beforeEntity로 전달
                    recursiveMapper.accept(null, newAfterEntity);
                } else if (Boolean.TRUE
                        .equals(isAffectedExtractor.apply(newAfterEntity))) {
                    // requestIdMap에 없지만 isAffected가 true인 경우
                    // 전파로 생성된 엔티티이므로 하위 엔티티만 재귀적으로 매핑
                    recursiveMapper.accept(null, newAfterEntity);
                }
            }
        }
    }

    /**
     * 모든 엔티티 타입의 ID를 전파하면서 스키마를 매핑
     */
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
                    // beforeSchema가 null인 경우 (새로 생성된 스키마) 빈 스키마로 처리
                    Validation.Schema beforeSchemaSafe = beforeSchema != null
                            ? beforeSchema
                            : Validation.Schema.getDefaultInstance();
                    mapTablesWithMaps(builder, beforeSchemaSafe, afterSchema,
                            requestIdMaps);
                },
                requestIdMaps.schemas);
    }

    /**
     * 모든 엔티티 타입의 ID를 전파하면서 테이블을 매핑
     * 컬럼 생성 시 다른 테이블의 컬럼, 제약조건, 관계, 인덱스 등에 영향을 미칠 수 있음
     */
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
                    // 모든 하위 엔티티에 requestIdMaps 전달
                    // beforeTable이 null인 경우 (새로 생성된 테이블) 빈 테이블로 처리
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

    /**
     * 컬럼 ID를 전파하면서 매핑
     * 컬럼 생성 시 해당 테이블뿐만 아니라 FK 관계로 인해 다른 테이블의 컬럼도 생성될 수 있음
     */
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

    /**
     * 인덱스 ID를 전파하면서 매핑
     * 컬럼 생성 시 PK/UNIQUE 제약조건으로 인해 인덱스가 자동 생성될 수 있음
     */
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

    /**
     * 인덱스 컬럼 ID를 전파하면서 매핑
     */
    private static void mapIndexColumnsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Index beforeIndex,
            Validation.Index afterIndex,
            RequestIdMaps requestIdMaps) {
        // beforeIndex가 null인 경우 (새로 생성된 인덱스) 빈 인덱스로 처리
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

    /**
     * 제약조건 ID를 전파하면서 매핑
     * 컬럼 생성 시 PK/UNIQUE/FK 등의 제약조건이 자동 생성될 수 있음
     */
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

    /**
     * 제약조건 컬럼 ID를 전파하면서 매핑
     */
    private static void mapConstraintColumnsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Constraint beforeConstraint,
            Validation.Constraint afterConstraint,
            RequestIdMaps requestIdMaps) {
        // beforeConstraint가 null인 경우 (새로 생성된 제약조건) 빈 제약조건으로 처리
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

    /**
     * 관계 ID를 전파하면서 매핑
     * 컬럼 생성 시 FK 컬럼이면 관계(Relationship)가 자동 생성될 수 있음
     */
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

    /**
     * 관계 컬럼 ID를 전파하면서 매핑
     */
    private static void mapRelationshipColumnsWithMaps(
            AffectedMappingResponseBuilder builder,
            Validation.Relationship beforeRelationship,
            Validation.Relationship afterRelationship,
            RequestIdMaps requestIdMaps) {
        // beforeRelationship이 null인 경우 (새로 생성된 관계) 빈 관계로 처리
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
