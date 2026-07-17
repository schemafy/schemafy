package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReorderPositions")
class ReorderPositionsTest {

  @Test
  @DisplayName("현재 entity 순서를 position snapshot으로 캡처한다")
  void capture_mapsEntityIdsAndPositions() {
    List<PositionedEntity> entities = List.of(
        new PositionedEntity("entity-1", 0),
        new PositionedEntity("entity-2", 1));

    List<ReorderPosition> positions = ReorderPositions.capture(
        entities,
        PositionedEntity::id,
        PositionedEntity::seqNo);

    assertThat(positions).containsExactly(
        new ReorderPosition("entity-1", 0),
        new ReorderPosition("entity-2", 1));
  }

  @Test
  @DisplayName("현재 entity와 일치하는 snapshot을 id별 position으로 변환한다")
  void indexForRestore_returnsPositionsById() {
    List<PositionedEntity> currentEntities = List.of(
        new PositionedEntity("entity-2", 0),
        new PositionedEntity("entity-1", 1));
    List<ReorderPosition> snapshot = List.of(
        new ReorderPosition("entity-1", 0),
        new ReorderPosition("entity-2", 1));

    var positionsById = ReorderPositions.indexForRestore(
        currentEntities,
        PositionedEntity::id,
        snapshot);

    assertThat(positionsById)
        .containsEntry("entity-1", 0)
        .containsEntry("entity-2", 1);
  }

  @Test
  @DisplayName("snapshot 크기, 중복 id, 현재 entity id가 일치하지 않으면 거부한다")
  void indexForRestore_rejectsSnapshotMismatch() {
    List<PositionedEntity> currentEntities = List.of(
        new PositionedEntity("entity-1", 0),
        new PositionedEntity("entity-2", 1));

    assertThatThrownBy(() -> ReorderPositions.indexForRestore(
        currentEntities,
        PositionedEntity::id,
        List.of(new ReorderPosition("entity-1", 0))))
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(() -> ReorderPositions.indexForRestore(
        currentEntities,
        PositionedEntity::id,
        List.of(
            new ReorderPosition("entity-1", 0),
            new ReorderPosition("entity-1", 1))))
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(() -> ReorderPositions.indexForRestore(
        currentEntities,
        PositionedEntity::id,
        List.of(
            new ReorderPosition("entity-1", 0),
            new ReorderPosition("entity-3", 1))))
        .isInstanceOf(IllegalStateException.class);
  }

  private record PositionedEntity(String id, int seqNo) {
  }

}
