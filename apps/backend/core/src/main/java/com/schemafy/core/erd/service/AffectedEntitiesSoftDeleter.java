package com.schemafy.core.erd.service;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.type.BaseEntity;
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
public class AffectedEntitiesSoftDeleter {

    private final SchemaRepository schemaRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final IndexRepository indexRepository;
    private final IndexColumnRepository indexColumnRepository;
    private final ConstraintRepository constraintRepository;
    private final ConstraintColumnRepository constraintColumnRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipColumnRepository relationshipColumnRepository;

    public Mono<Void> softDeleteRemovedEntities(
            Validation.Database before,
            Validation.Database after) {
        ValidationDatabaseEntityIds beforeIds = ValidationDatabaseEntityIds
                .from(before);
        ValidationDatabaseEntityIds afterIds = ValidationDatabaseEntityIds
                .from(after);

        Set<String> removedRelationshipColumns = difference(
                beforeIds.relationshipColumns(),
                afterIds.relationshipColumns());
        Set<String> removedConstraintColumns = difference(
                beforeIds.constraintColumns(),
                afterIds.constraintColumns());
        Set<String> removedIndexColumns = difference(beforeIds.indexColumns(),
                afterIds.indexColumns());
        Set<String> removedRelationships = difference(beforeIds.relationships(),
                afterIds.relationships());
        Set<String> removedConstraints = difference(beforeIds.constraints(),
                afterIds.constraints());
        Set<String> removedIndexes = difference(beforeIds.indexes(),
                afterIds.indexes());
        Set<String> removedColumns = difference(beforeIds.columns(),
                afterIds.columns());
        Set<String> removedTables = difference(beforeIds.tables(),
                afterIds.tables());
        Set<String> removedSchemas = difference(beforeIds.schemas(),
                afterIds.schemas());

        return softDeleteEntities(new ValidationDatabaseEntityIds(
                removedSchemas,
                removedTables,
                removedColumns,
                removedIndexes,
                removedIndexColumns,
                removedConstraints,
                removedConstraintColumns,
                removedRelationships,
                removedRelationshipColumns));
    }

    public Mono<Void> softDeleteEntities(ValidationDatabaseEntityIds idsToDelete) {
        return softDelete(idsToDelete.relationshipColumns(),
                relationshipColumnRepository::findByIdAndDeletedAtIsNull,
                relationshipColumnRepository::save)
                .then(softDelete(idsToDelete.constraintColumns(),
                        constraintColumnRepository::findByIdAndDeletedAtIsNull,
                        constraintColumnRepository::save))
                .then(softDelete(idsToDelete.indexColumns(),
                        indexColumnRepository::findByIdAndDeletedAtIsNull,
                        indexColumnRepository::save))
                .then(softDelete(idsToDelete.relationships(),
                        relationshipRepository::findByIdAndDeletedAtIsNull,
                        relationshipRepository::save))
                .then(softDelete(idsToDelete.constraints(),
                        constraintRepository::findByIdAndDeletedAtIsNull,
                        constraintRepository::save))
                .then(softDelete(idsToDelete.indexes(),
                        indexRepository::findByIdAndDeletedAtIsNull,
                        indexRepository::save))
                .then(softDelete(idsToDelete.columns(),
                        columnRepository::findByIdAndDeletedAtIsNull,
                        columnRepository::save))
                .then(softDelete(idsToDelete.tables(),
                        tableRepository::findByIdAndDeletedAtIsNull,
                        tableRepository::save))
                .then(softDelete(idsToDelete.schemas(),
                        schemaRepository::findByIdAndDeletedAtIsNull,
                        schemaRepository::save));
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private static <T extends BaseEntity> Mono<Void> softDelete(
            Set<String> ids,
            Function<String, Mono<T>> finder,
            Function<T, Mono<T>> saver) {
        if (ids.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(ids)
                .concatMap(id -> finder.apply(id)
                        .doOnNext(BaseEntity::delete)
                        .flatMap(saver))
                .then();
    }
}
