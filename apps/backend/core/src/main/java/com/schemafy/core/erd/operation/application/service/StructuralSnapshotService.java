package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StructuralSnapshotService {

  private final StructuralSnapshotReader reader;
  private final StructuralSnapshotReconciler reconciler;

  public Mono<StructuralSnapshot> captureByConstraintId(String constraintId) {
    return reader.captureByConstraintId(constraintId);
  }

  public Mono<StructuralSnapshot> captureByConstraintColumnId(String constraintColumnId) {
    return reader.captureByConstraintColumnId(constraintColumnId);
  }

  public Mono<StructuralSnapshot> captureByIndexId(String indexId) {
    return reader.captureByIndexId(indexId);
  }

  public Mono<StructuralSnapshot> captureByIndexColumnId(String indexColumnId) {
    return reader.captureByIndexColumnId(indexColumnId);
  }

  public Mono<StructuralSnapshot> captureByRelationshipId(String relationshipId) {
    return reader.captureByRelationshipId(relationshipId);
  }

  public Mono<StructuralSnapshot> captureByRelationshipColumnId(String relationshipColumnId) {
    return reader.captureByRelationshipColumnId(relationshipColumnId);
  }

  public Mono<StructuralSnapshot> captureByTableId(String tableId) {
    return reader.captureByTableId(tableId);
  }

  public Mono<StructuralSnapshot> captureBySchemaId(String schemaId) {
    return reader.captureBySchemaId(schemaId);
  }

  public Mono<Void> reconcileTo(StructuralSnapshot target) {
    return reconciler.reconcileTo(target);
  }

}
