package com.schemafy.core.erd.constraint.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.core.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.constraint.fixture.ConstraintFixture;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.CONSTRAINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeConstraintNameService")
class ChangeConstraintNameServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";

  @Mock
  ChangeConstraintNamePort changeConstraintNamePort;

  @Mock
  ConstraintExistsPort constraintExistsPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  IdentifierCapabilityResolver identifierCapabilityResolver;

  @InjectMocks
  ChangeConstraintNameService sut;

  @BeforeEach
  void setUpIdentifierCapabilities() {
    org.mockito.Mockito.lenient()
        .when(identifierCapabilityResolver.resolve(any(), anyString()))
        .thenReturn(Mono.just(IdentifierCapabilities.codePoints(64)));
  }

  @Nested
  @DisplayName("changeConstraintName 메서드는")
  class ChangeConstraintName {

    @Test
    @DisplayName("유효한 새 이름으로 변경한다")
    void changesNameWithValidNewName() {
      var command = ConstraintFixture.changeNameCommand("new_constraint_name");
      var constraint = ConstraintFixture.defaultConstraint();
      var table = createTable(constraint.tableId(), SCHEMA_ID);

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getTableByIdPort.findTableById(any()))
          .willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndNameExcludingId(any(), any(), any()))
          .willReturn(Mono.just(false));
      given(changeConstraintNamePort.changeConstraintName(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintName(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeConstraintNamePort).should()
          .changeConstraintName(eq(constraint.id()), eq("new_constraint_name"));
    }

    @Test
    @DisplayName("DB vendor 제한과 같은 64 코드 포인트 이름으로 변경한다")
    void changesNameAtVendorLimit() {
      String newName = "😀".repeat(64);
      var command = ConstraintFixture.changeNameCommand(newName);
      var constraint = ConstraintFixture.defaultConstraint();
      var table = createTable(constraint.tableId(), SCHEMA_ID);

      given(getConstraintByIdPort.findConstraintById(command.constraintId()))
          .willReturn(Mono.just(constraint));
      given(getTableByIdPort.findTableById(constraint.tableId()))
          .willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndNameExcludingId(
          table.schemaId(), newName, constraint.id()))
          .willReturn(Mono.just(false));
      given(changeConstraintNamePort.changeConstraintName(constraint.id(), newName))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintName(command))
          .expectNextCount(1)
          .verifyComplete();

      then(identifierCapabilityResolver).should().resolve(CONSTRAINT, constraint.id());
      then(changeConstraintNamePort).should().changeConstraintName(constraint.id(), newName);
    }

    @Test
    @DisplayName("DB vendor 제한을 넘는 65 코드 포인트 이름 변경을 거부한다")
    void rejectsNameOverVendorLimit() {
      String newName = "😀".repeat(65);
      var command = ConstraintFixture.changeNameCommand(newName);
      var constraint = ConstraintFixture.defaultConstraint();

      given(getConstraintByIdPort.findConstraintById(command.constraintId()))
          .willReturn(Mono.just(constraint));

      StepVerifier.create(sut.changeConstraintName(command))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.NAME_INVALID))
          .verify();

      then(identifierCapabilityResolver).should().resolve(CONSTRAINT, constraint.id());
      then(getTableByIdPort).shouldHaveNoInteractions();
      then(constraintExistsPort).shouldHaveNoInteractions();
      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("현재 이름과 같으면 table과 중복 조회 없이 변경 없이 성공한다")
    void succeedsWithoutContextLookupWhenNameIsSame() {
      var command = ConstraintFixture.changeNameCommand(ConstraintFixture.DEFAULT_NAME);
      var constraint = ConstraintFixture.defaultConstraint();

      given(getConstraintByIdPort.findConstraintById(command.constraintId()))
          .willReturn(Mono.just(constraint));

      StepVerifier.create(sut.changeConstraintName(command))
          .expectNextMatches(result -> result.operation() == null)
          .verifyComplete();

      then(getTableByIdPort).shouldHaveNoInteractions();
      then(constraintExistsPort).shouldHaveNoInteractions();
      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("빈 이름이면 예외가 발생한다")
    void throwsWhenEmptyName() {
      var command = ConstraintFixture.changeNameCommand("");

      StepVerifier.create(sut.changeConstraintName(command))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.NAME_INVALID))
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("공백 이름이면 예외가 발생한다")
    void throwsWhenBlankName() {
      var command = ConstraintFixture.changeNameCommand("   ");

      StepVerifier.create(sut.changeConstraintName(command))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.NAME_INVALID))
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      var command = ConstraintFixture.changeNameCommand("new_name");

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintName(command))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.NOT_FOUND))
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 중복되면 예외가 발생한다")
    void throwsWhenNameDuplicate() {
      var command = ConstraintFixture.changeNameCommand("duplicate_name");
      var constraint = ConstraintFixture.defaultConstraint();
      var table = createTable(constraint.tableId(), SCHEMA_ID);

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getTableByIdPort.findTableById(any()))
          .willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndNameExcludingId(any(), any(), any()))
          .willReturn(Mono.just(true));

      StepVerifier.create(sut.changeConstraintName(command))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.NAME_DUPLICATE))
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

  }

  private Table createTable(String tableId, String schemaId) {
    return new Table(tableId, schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci");
  }

}
