package com.schemafy.domain.erd.index.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetIndexColumnsByIndexIdService")
class GetIndexColumnsByIndexIdServiceTest {

  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @InjectMocks
  GetIndexColumnsByIndexIdService sut;

  @Nested
  @DisplayName("getIndexColumnsByIndexId 메서드는")
  class GetIndexColumnsByIndexId {

    @Test
    @DisplayName("인덱스 ID로 컬럼 목록을 반환한다")
    void returnsColumnsByIndexId() {
      var query = IndexFixture.getIndexColumnsByIndexIdQuery("idx1");
      var columns = List.of(
          IndexFixture.indexColumn("ic1", "idx1", "col1", 0, SortDirection.ASC),
          IndexFixture.indexColumn("ic2", "idx1", "col2", 1, SortDirection.DESC));

      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("idx1"))
          .willReturn(Mono.just(columns));

      StepVerifier.create(sut.getIndexColumnsByIndexId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo("ic1");
            assertThat(result.get(0).sortDirection()).isEqualTo(SortDirection.ASC);
            assertThat(result.get(1).id()).isEqualTo("ic2");
            assertThat(result.get(1).sortDirection()).isEqualTo(SortDirection.DESC);
          })
          .verifyComplete();

      then(getIndexColumnsByIndexIdPort).should().findIndexColumnsByIndexId("idx1");
    }

    @Test
    @DisplayName("컬럼이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoColumns() {
      var query = IndexFixture.getIndexColumnsByIndexIdQuery("idx1");

      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("idx1"))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.getIndexColumnsByIndexId(query))
          .assertNext(result -> assertThat(result).isEmpty())
          .verifyComplete();
    }
  }

}
