package com.schemafy.domain.erd.index.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetIndexesByTableIdService")
class GetIndexesByTableIdServiceTest {

  @Mock
  GetIndexesByTableIdPort getIndexesByTableIdPort;

  @InjectMocks
  GetIndexesByTableIdService sut;

  @Nested
  @DisplayName("getIndexesByTableId 메서드는")
  class GetIndexesByTableId {

    @Test
    @DisplayName("테이블 ID로 인덱스 목록을 반환한다")
    void returnsIndexesByTableId() {
      var query = IndexFixture.getIndexesByTableIdQuery("table1");
      var indexes = List.of(
          IndexFixture.indexWithIdAndName("idx1", "idx_name1"),
          IndexFixture.indexWithIdAndName("idx2", "idx_name2"));

      given(getIndexesByTableIdPort.findIndexesByTableId("table1"))
          .willReturn(Mono.just(indexes));

      StepVerifier.create(sut.getIndexesByTableId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo("idx1");
            assertThat(result.get(1).id()).isEqualTo("idx2");
          })
          .verifyComplete();

      then(getIndexesByTableIdPort).should().findIndexesByTableId("table1");
    }

    @Test
    @DisplayName("인덱스가 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoIndexes() {
      var query = IndexFixture.getIndexesByTableIdQuery("table1");

      given(getIndexesByTableIdPort.findIndexesByTableId("table1"))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.getIndexesByTableId(query))
          .assertNext(result -> assertThat(result).isEmpty())
          .verifyComplete();
    }

  }

}
