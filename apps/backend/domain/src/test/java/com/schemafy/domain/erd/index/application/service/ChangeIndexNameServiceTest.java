package com.schemafy.domain.erd.index.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.ChangeIndexNamePort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.IndexExistsPort;
import com.schemafy.domain.erd.index.domain.exception.IndexNameDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexNameInvalidException;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeIndexNameService")
class ChangeIndexNameServiceTest {

  @Mock
  ChangeIndexNamePort changeIndexNamePort;

  @Mock
  IndexExistsPort indexExistsPort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @InjectMocks
  ChangeIndexNameService sut;

  @Nested
  @DisplayName("changeIndexName 메서드는")
  class ChangeIndexName {

    @Test
    @DisplayName("유효한 이름으로 변경한다")
    void changesNameWithValidInput() {
      var command = IndexFixture.changeNameCommand("index1", "new_index_name");
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(indexExistsPort.existsByTableIdAndNameExcludingId(index.tableId(), "new_index_name", "index1"))
          .willReturn(Mono.just(false));
      given(changeIndexNamePort.changeIndexName("index1", "new_index_name"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexName(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeIndexNamePort).should().changeIndexName("index1", "new_index_name");
    }

    @Test
    @DisplayName("이름이 null이면 예외가 발생한다")
    void throwsWhenNameIsNull() {
      var command = IndexFixture.changeNameCommand("index1", null);

      StepVerifier.create(sut.changeIndexName(command))
          .expectError(IndexNameInvalidException.class)
          .verify();

      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 예외가 발생한다")
    void throwsWhenNameIsEmpty() {
      var command = IndexFixture.changeNameCommand("index1", "  ");

      StepVerifier.create(sut.changeIndexName(command))
          .expectError(IndexNameInvalidException.class)
          .verify();

      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인덱스가 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      var command = IndexFixture.changeNameCommand("nonexistent", "new_name");

      given(getIndexByIdPort.findIndexById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexName(command))
          .expectError(IndexNotExistException.class)
          .verify();

      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복된 이름이면 예외가 발생한다")
    void throwsWhenNameIsDuplicate() {
      var command = IndexFixture.changeNameCommand("index1", "existing_name");
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(indexExistsPort.existsByTableIdAndNameExcludingId(index.tableId(), "existing_name", "index1"))
          .willReturn(Mono.just(true));

      StepVerifier.create(sut.changeIndexName(command))
          .expectError(IndexNameDuplicateException.class)
          .verify();

      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름 앞뒤 공백을 제거한다")
    void trimsWhitespace() {
      var command = IndexFixture.changeNameCommand("index1", "  trimmed_name  ");
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(indexExistsPort.existsByTableIdAndNameExcludingId(index.tableId(), "trimmed_name", "index1"))
          .willReturn(Mono.just(false));
      given(changeIndexNamePort.changeIndexName("index1", "trimmed_name"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexName(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeIndexNamePort).should().changeIndexName("index1", "trimmed_name");
    }

  }

}
