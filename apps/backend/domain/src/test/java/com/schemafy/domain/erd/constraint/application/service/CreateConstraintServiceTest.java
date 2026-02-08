package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnCountInvalidException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintDefinitionDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.MultiplePrimaryKeyConstraintException;
import com.schemafy.domain.erd.constraint.domain.exception.UniqueSameAsPrimaryKeyException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateConstraintService")
class CreateConstraintServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateConstraintPort createConstraintPort;

  @Mock
  CreateConstraintColumnPort createConstraintColumnPort;

  @Mock
  ConstraintExistsPort constraintExistsPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Mock
  PkCascadeHelper pkCascadeHelper;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  CreateConstraintService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("createConstraint 메서드는")
  class CreateConstraint {

    @Test
    @DisplayName("유효한 PRIMARY KEY 제약조건을 생성한다")
    void createsPrimaryKeyConstraint() {
      var command = ConstraintFixture.createPrimaryKeyCommandWithColumns(
          List.of(ConstraintFixture.createColumnCommand("col1", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById("col1"))
          .willReturn(Mono.just(ColumnFixture.columnWithId("col1")));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(
              result -> {
                assertThat(result.result().constraintId()).isEqualTo("new-constraint-id");
                assertThat(result.result().name()).isEqualTo("pk_test");
                assertThat(result.result().kind()).isEqualTo(ConstraintKind.PRIMARY_KEY);
              })
          .verifyComplete();

      then(createConstraintPort).should().createConstraint(any(Constraint.class));
      then(createConstraintColumnPort).should().createConstraintColumn(any(ConstraintColumn.class));
    }

    @Test
    @DisplayName("유효한 UNIQUE 제약조건을 생성한다")
    void createsUniqueConstraint() {
      var command = ConstraintFixture.createUniqueCommandWithColumns(
          List.of(
              ConstraintFixture.createColumnCommand("col1", 0),
              ConstraintFixture.createColumnCommand("col2", 1)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"), ColumnFixture.columnWithId("col2"));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "uq_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "cc1", "cc2");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(
              result -> {
                assertThat(result.result().constraintId()).isEqualTo("new-constraint-id");
                assertThat(result.result().name()).isEqualTo("uq_test");
                assertThat(result.result().kind()).isEqualTo(ConstraintKind.UNIQUE);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("컬럼 seqNo가 없으면 0부터 자동 배정한다")
    void createsConstraintWithAutoSeqNosWhenMissing() {
      CreateConstraintCommand command = new CreateConstraintCommand(
          "table1",
          "uq_auto",
          ConstraintKind.UNIQUE,
          null,
          null,
          List.of(
              new CreateConstraintColumnCommand("col1", null),
              new CreateConstraintColumnCommand("col2", null)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"), ColumnFixture.columnWithId("col2"));
      List<ConstraintColumn> capturedColumns = new java.util.ArrayList<>();

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "uq_auto"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "cc1", "cc2");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> {
            ConstraintColumn column = invocation.getArgument(0);
            capturedColumns.add(column);
            return Mono.just(column);
          });

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(result -> assertThat(result.result().constraintId()).isEqualTo("new-constraint-id"))
          .verifyComplete();

      assertThat(capturedColumns).extracting(ConstraintColumn::seqNo).containsExactly(0, 1);
    }

    @Test
    @DisplayName("이름이 null이면 자동 생성한다")
    void autoGeneratesNameWhenNull() {
      var command = ConstraintFixture.createCommandWithName(null);
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test_table"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById(ConstraintFixture.DEFAULT_COLUMN_ID))
          .willReturn(Mono.just(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID)));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(result -> {
            assertThat(result.result().name()).isEqualTo("pk_test_table");
            assertThat(result.result().kind()).isEqualTo(ConstraintKind.PRIMARY_KEY);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 자동 생성한다")
    void autoGeneratesNameWhenEmpty() {
      var command = ConstraintFixture.createCommandWithName("  ");
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test_table"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById(ConstraintFixture.DEFAULT_COLUMN_ID))
          .willReturn(Mono.just(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID)));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(result -> {
            assertThat(result.result().name()).isEqualTo("pk_test_table");
            assertThat(result.result().kind()).isEqualTo(ConstraintKind.PRIMARY_KEY);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("자동 생성 이름이 중복이면 suffix를 증가시킨다")
    void autoGeneratesNameWithSuffixWhenDuplicate() {
      var command = ConstraintFixture.createCommandWithName(null);
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test_table"))
          .willReturn(Mono.just(true));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test_table_1"))
          .willReturn(Mono.just(true));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test_table_2"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById(ConstraintFixture.DEFAULT_COLUMN_ID))
          .willReturn(Mono.just(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID)));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(result -> assertThat(result.result().name()).isEqualTo("pk_test_table_2"))
          .verifyComplete();
    }

    @Test
    @DisplayName("테이블이 존재하지 않으면 예외가 발생한다")
    void throwsWhenTableNotExists() {
      var command = ConstraintFixture.createPrimaryKeyCommand();

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.empty());

      StepVerifier.create(sut.createConstraint(command))
          .expectError(TableNotExistException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복된 이름이면 예외가 발생한다")
    void throwsWhenNameIsDuplicate() {
      var command = ConstraintFixture.createPrimaryKeyCommand();
      var table = createTable("table1", "schema1");

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test"))
          .willReturn(Mono.just(true));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(ConstraintNameDuplicateException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 테이블에 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExistsInTable() {
      var command = ConstraintFixture.createPrimaryKeyCommandWithColumns(
          List.of(ConstraintFixture.createColumnCommand("nonexistent", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(ConstraintColumnNotExistException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복 컬럼이 포함되면 예외가 발생한다")
    void throwsWhenDuplicateColumns() {
      var command = ConstraintFixture.createPrimaryKeyCommandWithColumns(
          List.of(
              ConstraintFixture.createColumnCommand("col1", 0),
              ConstraintFixture.createColumnCommand("col1", 1)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(ConstraintColumnDuplicateException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PK가 이미 존재하면 예외가 발생한다")
    void throwsWhenPrimaryKeyAlreadyExists() {
      var command = ConstraintFixture.createPrimaryKeyCommandWithColumns(
          List.of(ConstraintFixture.createColumnCommand("col1", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));
      var existingPk = new Constraint(
          "existing-pk", table.id(), "existing_pk", ConstraintKind.PRIMARY_KEY, null, null);

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "pk_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of(existingPk)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("existing-pk"))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(MultiplePrimaryKeyConstraintException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 컬럼 조합의 제약조건이 이미 존재하면 예외가 발생한다")
    void throwsWhenDefinitionAlreadyExists() {
      var command = ConstraintFixture.createUniqueCommandWithColumns(
          List.of(ConstraintFixture.createColumnCommand("col1", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));
      var existingConstraint = new Constraint(
          "existing-uq", table.id(), "existing_uq", ConstraintKind.UNIQUE, null, null);
      var existingColumnMapping = Map.of(
          "existing-uq", List.of(new ConstraintColumn("cc1", "existing-uq", "col1", 0)));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "uq_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of(existingConstraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("existing-uq"))
          .willReturn(Mono.just(existingColumnMapping.get("existing-uq")));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(ConstraintDefinitionDuplicateException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("UNIQUE가 PK와 같은 컬럼 조합이면 예외가 발생한다")
    void throwsWhenUniqueSameAsPrimaryKey() {
      var command = ConstraintFixture.createUniqueCommandWithColumns(
          List.of(ConstraintFixture.createColumnCommand("col1", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));
      var existingPk = new Constraint(
          "existing-pk", table.id(), "existing_pk", ConstraintKind.PRIMARY_KEY, null, null);
      var pkColumnMapping = Map.of("existing-pk", List.of(new ConstraintColumn("cc1", "existing-pk", "col1", 0)));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "uq_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of(existingPk)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("existing-pk"))
          .willReturn(Mono.just(pkColumnMapping.get("existing-pk")));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(UniqueSameAsPrimaryKeyException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("kind가 null이면 예외가 발생한다")
    void throwsWhenKindIsNull() {
      var command = ConstraintFixture.createCommandWithKind(null);
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName(anyString(), anyString()))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(InvalidValueException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 없으면 컬럼 생성 없이 제약조건만 생성한다")
    void createsConstraintWithoutColumnsWhenEmpty() {
      var command = ConstraintFixture.createCommandWithColumns(List.of());
      var table = createTable("table1", "schema1");

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", command.name()))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(result -> assertThat(result.result().constraintId()).isEqualTo("new-constraint-id"))
          .verifyComplete();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("CHECK 제약조건에 checkExpr을 포함한다")
    void createsCheckConstraintWithExpression() {
      var command = ConstraintFixture.createCheckCommand("value > 0");
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "ck_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(
              result -> {
                assertThat(result.result().kind()).isEqualTo(ConstraintKind.CHECK);
                assertThat(result.result().checkExpr()).isEqualTo("value > 0");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("DEFAULT 제약조건에 defaultExpr을 포함한다")
    void createsDefaultConstraintWithExpression() {
      var command = ConstraintFixture.createDefaultCommand("0");
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "df_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(
              result -> {
                assertThat(result.result().kind()).isEqualTo(ConstraintKind.DEFAULT);
                assertThat(result.result().defaultExpr()).isEqualTo("0");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("DEFAULT 제약조건의 컬럼이 2개 이상이면 예외가 발생한다")
    void throwsWhenDefaultHasMultipleColumns() {
      var command = new CreateConstraintCommand(
          ConstraintFixture.DEFAULT_TABLE_ID,
          "df_test",
          ConstraintKind.DEFAULT,
          null,
          "0",
          List.of(
              new CreateConstraintColumnCommand("col1", 0),
              new CreateConstraintColumnCommand("col2", 1)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "df_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createConstraint(command))
          .expectError(ConstraintColumnCountInvalidException.class)
          .verify();

      then(createConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("CHECK 제약조건은 checkExpr 없이 생성할 수 있다")
    void createsCheckConstraintWithoutExpression() {
      var command = ConstraintFixture.createCheckCommand(null);
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "ck_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(
              result -> {
                assertThat(result.result().kind()).isEqualTo(ConstraintKind.CHECK);
                assertThat(result.result().checkExpr()).isNull();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("DEFAULT 제약조건은 defaultExpr 없이 생성할 수 있다")
    void createsDefaultConstraintWithoutExpression() {
      var command = ConstraintFixture.createDefaultCommand(null);
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(ConstraintFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId())).willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndName("schema1", "df_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-constraint-id", "new-column-id");
      given(createConstraintPort.createConstraint(any(Constraint.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createConstraint(command))
          .assertNext(
              result -> {
                assertThat(result.result().kind()).isEqualTo(ConstraintKind.DEFAULT);
                assertThat(result.result().defaultExpr()).isNull();
              })
          .verifyComplete();
    }

  }

  private Table createTable(String id, String schemaId) {
    return new Table(id, schemaId, "test_table", null, null);
  }

}
