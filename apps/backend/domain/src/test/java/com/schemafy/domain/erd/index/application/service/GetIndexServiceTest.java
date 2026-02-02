package com.schemafy.domain.erd.index.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetIndexService")
class GetIndexServiceTest {

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @InjectMocks
  GetIndexService sut;

  @Nested
  @DisplayName("getIndex 메서드는")
  class GetIndex {

    @Test
    @DisplayName("인덱스가 존재하면 반환한다")
    void returnsIndexWhenExists() {
      var query = IndexFixture.getIndexQuery("index1");
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));

      StepVerifier.create(sut.getIndex(query))
          .assertNext(result -> {
            assertThat(result.id()).isEqualTo("index1");
            assertThat(result.name()).isEqualTo(index.name());
          })
          .verifyComplete();

      then(getIndexByIdPort).should().findIndexById("index1");
    }

    @Test
    @DisplayName("인덱스가 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      var query = IndexFixture.getIndexQuery("nonexistent");

      given(getIndexByIdPort.findIndexById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getIndex(query))
          .expectError(IndexNotExistException.class)
          .verify();
    }

  }

}
