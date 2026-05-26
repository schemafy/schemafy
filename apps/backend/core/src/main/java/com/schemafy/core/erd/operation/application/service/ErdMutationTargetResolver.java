package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ErdMutationTargetResolver {

  private final SchemaMutationTargetResolver schemaTargetResolver;
  private final TableMutationTargetResolver tableTargetResolver;
  private final ColumnMutationTargetResolver columnTargetResolver;
  private final ConstraintMutationTargetResolver constraintTargetResolver;
  private final IndexMutationTargetResolver indexTargetResolver;
  private final RelationshipMutationTargetResolver relationshipTargetResolver;

  Mono<ResolvedErdMutationTarget> resolveBefore(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
    case CREATE_SCHEMA, CHANGE_SCHEMA_NAME, DELETE_SCHEMA -> schemaTargetResolver.resolve(operationType, payload);
    case CREATE_TABLE, CHANGE_TABLE_NAME, CHANGE_TABLE_META, CHANGE_TABLE_EXTRA, DELETE_TABLE ->
      tableTargetResolver.resolve(operationType, payload);
    case CREATE_COLUMN, CHANGE_COLUMN_NAME, CHANGE_COLUMN_TYPE, CHANGE_COLUMN_META, CHANGE_COLUMN_POSITION,
        DELETE_COLUMN -> columnTargetResolver.resolve(operationType, payload);
    case CREATE_CONSTRAINT, CHANGE_CONSTRAINT_NAME, CHANGE_CONSTRAINT_CHECK_EXPR,
        CHANGE_CONSTRAINT_DEFAULT_EXPR, DELETE_CONSTRAINT, ADD_CONSTRAINT_COLUMN, REMOVE_CONSTRAINT_COLUMN,
        CHANGE_CONSTRAINT_COLUMN_POSITION -> constraintTargetResolver.resolve(operationType, payload);
    case CREATE_INDEX, CHANGE_INDEX_NAME, CHANGE_INDEX_TYPE, DELETE_INDEX, ADD_INDEX_COLUMN, REMOVE_INDEX_COLUMN,
        CHANGE_INDEX_COLUMN_POSITION, CHANGE_INDEX_COLUMN_SORT_DIRECTION ->
      indexTargetResolver.resolve(operationType, payload);
    case CREATE_RELATIONSHIP, CHANGE_RELATIONSHIP_NAME, CHANGE_RELATIONSHIP_KIND,
        CHANGE_RELATIONSHIP_CARDINALITY, CHANGE_RELATIONSHIP_EXTRA, DELETE_RELATIONSHIP, ADD_RELATIONSHIP_COLUMN,
        REMOVE_RELATIONSHIP_COLUMN, CHANGE_RELATIONSHIP_COLUMN_POSITION ->
      relationshipTargetResolver.resolve(operationType, payload);
    };
  }

}
