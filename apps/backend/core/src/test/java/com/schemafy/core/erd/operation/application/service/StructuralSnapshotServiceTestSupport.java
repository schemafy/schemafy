package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public final class StructuralSnapshotServiceTestSupport {

  private StructuralSnapshotServiceTestSupport() {}

  public static void stubEmptySnapshots(StructuralSnapshotService structuralSnapshotService) {
    StructuralSnapshot snapshot = emptySnapshot();
    Mono<StructuralSnapshot> snapshotMono = Mono.just(snapshot);

    lenient().when(structuralSnapshotService.captureByConstraintId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureByColumnId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureByConstraintColumnId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureByIndexId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureByIndexColumnId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureByRelationshipId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureByRelationshipColumnId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureByTableId(any()))
        .thenReturn(snapshotMono);
    lenient().when(structuralSnapshotService.captureBySchemaId(any()))
        .thenReturn(snapshotMono);
  }

  public static StructuralSnapshot emptySnapshot() {
    return new StructuralSnapshot(
        "schema1",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

}
