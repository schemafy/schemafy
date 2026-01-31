package com.schemafy.domain.erd.index.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteIndexService")
class DeleteIndexServiceTest {

  @Mock
  DeleteIndexPort deleteIndexPort;

  @Mock
  DeleteIndexColumnsByIndexIdPort deleteIndexColumnsPort;

  @InjectMocks
  DeleteIndexService sut;

  @Nested
  @DisplayName("deleteIndex 메서드는")
  class DeleteIndex {

    @Test
    @DisplayName("인덱스 컬럼을 먼저 삭제하고 인덱스를 삭제한다")
    void deletesColumnsFirstThenIndex() {
      var command = IndexFixture.deleteCommand("index1");

      given(deleteIndexColumnsPort.deleteByIndexId("index1"))
          .willReturn(Mono.empty());
      given(deleteIndexPort.deleteIndex("index1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteIndex(command))
          .verifyComplete();

      then(deleteIndexColumnsPort).should().deleteByIndexId("index1");
      then(deleteIndexPort).should().deleteIndex("index1");
    }

    @Test
    @DisplayName("컬럼이 없는 인덱스도 삭제한다")
    void deletesIndexWithoutColumns() {
      var command = IndexFixture.deleteCommand("index1");

      given(deleteIndexColumnsPort.deleteByIndexId("index1"))
          .willReturn(Mono.empty());
      given(deleteIndexPort.deleteIndex("index1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteIndex(command))
          .verifyComplete();

      then(deleteIndexPort).should().deleteIndex("index1");
    }

  }

}
