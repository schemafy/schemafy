package com.schemafy.core.erd.index.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexNamePort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.IndexExistsPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.index.fixture.IndexFixture;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

  @Mock
  IdentifierCapabilityResolver identifierCapabilityResolver;

  @InjectMocks
  ChangeIndexNameService sut;

  @BeforeEach
  void setUpIdentifierCapabilities() {
    org.mockito.Mockito.lenient()
        .when(identifierCapabilityResolver.resolve(any(), anyString()))
        .thenReturn(Mono.just(IdentifierCapabilities.codePoints(64)));
  }

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
    @DisplayName("DB vendor 제한과 같은 64 코드 포인트 이름으로 변경한다")
    void changesNameAtVendorLimit() {
      String newName = "😀".repeat(64);
      var command = IndexFixture.changeNameCommand("index1", newName);
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(indexExistsPort.existsByTableIdAndNameExcludingId(index.tableId(), newName, "index1"))
          .willReturn(Mono.just(false));
      given(changeIndexNamePort.changeIndexName("index1", newName))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexName(command))
          .expectNextCount(1)
          .verifyComplete();

      then(identifierCapabilityResolver).should().resolve(INDEX, index.id());
      then(changeIndexNamePort).should().changeIndexName("index1", newName);
    }

    @Test
    @DisplayName("DB vendor 제한을 넘는 65 코드 포인트 이름 변경을 거부한다")
    void rejectsNameOverVendorLimit() {
      String newName = "😀".repeat(65);
      var command = IndexFixture.changeNameCommand("index1", newName);
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));

      StepVerifier.create(sut.changeIndexName(command))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NAME_INVALID))
          .verify();

      then(identifierCapabilityResolver).should().resolve(INDEX, index.id());
      then(indexExistsPort).shouldHaveNoInteractions();
      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("현재 이름과 같으면 중복 조회 없이 변경 없이 성공한다")
    void returnsSuccessWithoutMutationWhenNameIsUnchanged() {
      var index = IndexFixture.indexWithId("index1");
      var command = IndexFixture.changeNameCommand("index1", index.name());

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));

      StepVerifier.create(sut.changeIndexName(command))
          .expectNextMatches(result -> result.operation() == null)
          .verifyComplete();

      then(indexExistsPort).shouldHaveNoInteractions();
      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("락 획득 후 이름이 이미 요청값이면 중복 조회 없이 no-op으로 성공한다")
    void returnsNoOpWhenLockedIndexAlreadyHasRequestedName() {
      var command = IndexFixture.changeNameCommand("index1", "new_index_name");
      var initialIndex = IndexFixture.indexWithId("index1");
      var lockedIndex = IndexFixture.index("index1", initialIndex.tableId(), "new_index_name", initialIndex.type());

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(initialIndex), Mono.just(lockedIndex));

      StepVerifier.create(sut.changeIndexName(command))
          .expectNextMatches(result -> result.noOp() && result.operation() == null)
          .verifyComplete();

      then(indexExistsPort).shouldHaveNoInteractions();
      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 null이면 예외가 발생한다")
    void throwsWhenNameIsNull() {
      var command = IndexFixture.changeNameCommand("index1", null);

      StepVerifier.create(sut.changeIndexName(command))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NAME_INVALID))
          .verify();

      then(changeIndexNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 예외가 발생한다")
    void throwsWhenNameIsEmpty() {
      var command = IndexFixture.changeNameCommand("index1", "  ");

      StepVerifier.create(sut.changeIndexName(command))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NAME_INVALID))
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
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NOT_FOUND))
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
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NAME_DUPLICATE))
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
