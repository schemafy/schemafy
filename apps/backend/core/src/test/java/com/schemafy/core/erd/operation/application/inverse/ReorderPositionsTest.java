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
        new PositionedEntity("entity-1", 0, "first"),
        new PositionedEntity("entity-2", 1, "second"));

    List<ReorderPosition> positions = ReorderPositions.capture(
        entities,
        PositionedEntity::id,
        PositionedEntity::seqNo);

    assertThat(positions).containsExactly(
        new ReorderPosition("entity-1", 0),
        new ReorderPosition("entity-2", 1));
  }

  @Test
  @DisplayName("현재 entity의 다른 값은 보존하고 snapshot position을 복원한다")
  void restore_appliesSnapshotPositions() {
    List<PositionedEntity> currentEntities = List.of(
        new PositionedEntity("entity-2", 0, "second"),
        new PositionedEntity("entity-1", 1, "first"));
    List<ReorderPosition> snapshot = List.of(
        new ReorderPosition("entity-1", 0),
        new ReorderPosition("entity-2", 1));

    List<PositionedEntity> restored = ReorderPositions.restore(
        currentEntities,
        PositionedEntity::id,
        snapshot,
        PositionedEntity::withSeqNo);

    assertThat(restored).containsExactly(
        new PositionedEntity("entity-2", 1, "second"),
        new PositionedEntity("entity-1", 0, "first"));
  }

  @Test
  @DisplayName("snapshot 크기, 중복 id, 현재 entity id가 일치하지 않으면 거부한다")
  void restore_rejectsSnapshotMismatch() {
    List<PositionedEntity> currentEntities = List.of(
        new PositionedEntity("entity-1", 0, "first"),
        new PositionedEntity("entity-2", 1, "second"));

    assertThatThrownBy(() -> ReorderPositions.restore(
        currentEntities,
        PositionedEntity::id,
        List.of(new ReorderPosition("entity-1", 0)),
        PositionedEntity::withSeqNo))
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(() -> ReorderPositions.restore(
        currentEntities,
        PositionedEntity::id,
        List.of(
            new ReorderPosition("entity-1", 0),
            new ReorderPosition("entity-1", 1)),
        PositionedEntity::withSeqNo))
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(() -> ReorderPositions.restore(
        currentEntities,
        PositionedEntity::id,
        List.of(
            new ReorderPosition("entity-1", 0),
            new ReorderPosition("entity-3", 1)),
        PositionedEntity::withSeqNo))
        .isInstanceOf(IllegalStateException.class);
  }

  private record PositionedEntity(String id, int seqNo, String label) {

    private PositionedEntity withSeqNo(int nextSeqNo) {
      return new PositionedEntity(id, nextSeqNo, label);
    }

  }

}
