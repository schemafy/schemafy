package com.schemafy.core.erd.index.domain.policy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.index.domain.type.IndexType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IndexCapabilities")
class IndexCapabilitiesTest {

  @Test
  @DisplayName("지원 타입과 정렬 방향 의미 타입을 제공한다")
  void exposesIndexCapabilities() {
    var capabilities = new IndexCapabilities(
        Set.of(IndexType.BTREE, IndexType.FULLTEXT, IndexType.SPATIAL),
        Set.of(IndexType.BTREE));

    assertThat(capabilities.supports(IndexType.BTREE)).isTrue();
    assertThat(capabilities.supports(IndexType.HASH)).isFalse();
    assertThat(capabilities.sortDirectionAffectsSemantics(IndexType.BTREE)).isTrue();
    assertThat(capabilities.sortDirectionAffectsSemantics(IndexType.FULLTEXT)).isFalse();
  }

  @Test
  @DisplayName("정렬 방향 의미 타입은 지원 타입에 포함되어야 한다")
  void rejectsSortDirectionTypeOutsideSupportedTypes() {
    assertThatThrownBy(() -> new IndexCapabilities(
        Set.of(IndexType.BTREE),
        Set.of(IndexType.FULLTEXT)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("입력 Set 변경으로 capability가 바뀌지 않는다")
  void copiesInputSets() {
    Set<IndexType> supportedTypes = new HashSet<>(Set.of(IndexType.BTREE));
    var capabilities = new IndexCapabilities(supportedTypes, Set.of(IndexType.BTREE));

    supportedTypes.add(IndexType.HASH);

    assertThat(capabilities.supports(IndexType.HASH)).isFalse();
    assertThatThrownBy(() -> capabilities.supportedTypes().add(IndexType.HASH))
        .isInstanceOf(UnsupportedOperationException.class);
  }

}
