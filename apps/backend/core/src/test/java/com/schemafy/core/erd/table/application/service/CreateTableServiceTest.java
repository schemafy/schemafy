package com.schemafy.core.erd.table.application.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.application.service.StructuralSnapshotService;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.schema.fixture.SchemaFixture;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.out.CreateTablePort;
import com.schemafy.core.erd.table.application.port.out.TableExistsPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.erd.table.fixture.TableFixture;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.erd.operation.application.service.StructuralSnapshotServiceTestSupport.stubEmptySnapshots;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTableService")
class CreateTableServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateTablePort createTablePort;

  @Mock
  TableExistsPort tableExistsPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  StructuralSnapshotService structuralSnapshotService;

  @Mock
  IdentifierCapabilityResolver identifierCapabilityResolver;

  @Spy
  JsonObjectMetadataConverter jsonObjectMetadataConverter = new JsonObjectMetadataConverter(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @InjectMocks
  CreateTableService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    stubEmptySnapshots(structuralSnapshotService);
    given(identifierCapabilityResolver.resolve(any(), any()))
        .willReturn(Mono.just(DbVendorFixture.defaultCapabilities().identifiers()));
  }

  @Nested
  @DisplayName("createTable 메서드는")
  class CreateTable {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("테이블을 생성하고 결과를 반환한다")
      void returnsCreatedTable() {
        var command = TableFixture.createCommand();
        var table = TableFixture.defaultTable();
        var schema = SchemaFixture.defaultSchema();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(ulidGeneratorPort.generate())
            .willReturn(TableFixture.DEFAULT_ID);
        given(createTablePort.createTable(any(Table.class)))
            .willReturn(Mono.just(table));

        StepVerifier.create(sut.createTable(command))
            .assertNext(result -> {
              var payload = result.result();
              assertThat(payload.tableId()).isEqualTo(TableFixture.DEFAULT_ID);
              assertThat(payload.name()).isEqualTo(command.name());
              assertThat(payload.charset()).isEqualTo(command.charset());
              assertThat(payload.collation()).isEqualTo(command.collation());
            })
            .verifyComplete();

        then(tableExistsPort).should()
            .existsBySchemaIdAndName(command.schemaId(), command.name());
        then(getSchemaByIdPort).should().findSchemaById(command.schemaId());
        then(createTablePort).should().createTable(any(Table.class));
      }

      @Test
      @DisplayName("DB Vendor identifier 최대 길이의 테이블을 생성한다")
      void createsTableAtIdentifierLimit() {
        var baseCommand = TableFixture.createCommand();
        var command = new CreateTableCommand(
            baseCommand.schemaId(),
            "t".repeat(64),
            baseCommand.charset(),
            baseCommand.collation(),
            baseCommand.extra());
        var schema = SchemaFixture.defaultSchema();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(ulidGeneratorPort.generate())
            .willReturn(TableFixture.DEFAULT_ID);
        given(createTablePort.createTable(any(Table.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createTable(command))
            .assertNext(result -> assertThat(result.result().name()).isEqualTo(command.name()))
            .verifyComplete();
      }

      @Test
      @DisplayName("extra JSON object를 core 경계에서 canonical 문자열로 저장한다")
      void canonicalizesExtraBeforePersisting() {
        var command = TableFixture.createCommandWithExtra(
            TableFixture.jsonObject("{\"ui\": {\"x\": 100, \"y\": 200}}"));
        var schema = SchemaFixture.defaultSchema();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(ulidGeneratorPort.generate())
            .willReturn(TableFixture.DEFAULT_ID);
        given(createTablePort.createTable(any(Table.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createTable(command))
            .assertNext(result -> assertThat(result.result().extra())
                .isEqualTo("{\"ui\":{\"x\":100,\"y\":200}}"))
            .verifyComplete();
      }

    }

    @Nested
    @DisplayName("DB Vendor identifier 최대 길이를 초과하면")
    class WithTooLongName {

      @Test
      @DisplayName("TableInvalidValueException을 발생시킨다")
      void rejectsTableOverIdentifierLimit() {
        var baseCommand = TableFixture.createCommand();
        var command = new CreateTableCommand(
            baseCommand.schemaId(),
            "t".repeat(65),
            baseCommand.charset(),
            baseCommand.collation(),
            baseCommand.extra());

        StepVerifier.create(sut.createTable(command))
            .expectErrorMatches(DomainException.hasErrorCode(TableErrorCode.INVALID_VALUE))
            .verify();

        then(tableExistsPort).shouldHaveNoInteractions();
        then(getSchemaByIdPort).shouldHaveNoInteractions();
        then(createTablePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("charset/collation이 비어 있으면")
    class WithEmptyMeta {

      @Test
      @DisplayName("스키마 메타를 기본값으로 상속한다")
      void inheritsSchemaMetaWhenBothMissing() {
        var command = TableFixture.createCommandWithoutMeta();
        var schema = SchemaFixture.defaultSchema();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(ulidGeneratorPort.generate())
            .willReturn(TableFixture.DEFAULT_ID);
        given(createTablePort.createTable(any(Table.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createTable(command))
            .assertNext(result -> {
              var payload = result.result();
              assertThat(payload.charset()).isEqualTo(schema.charset());
              assertThat(payload.collation()).isEqualTo(schema.collation());
            })
            .verifyComplete();
      }

      @Test
      @DisplayName("charset만 주어지면 collation은 스키마 값으로 상속한다")
      void inheritsSchemaCollationWhenOnlyCharsetGiven() {
        var command = TableFixture.createCommandWithMeta(" utf8mb4 ", " ");
        var schema = SchemaFixture.defaultSchema();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(ulidGeneratorPort.generate())
            .willReturn(TableFixture.DEFAULT_ID);
        given(createTablePort.createTable(any(Table.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createTable(command))
            .assertNext(result -> {
              var payload = result.result();
              assertThat(payload.charset()).isEqualTo("utf8mb4");
              assertThat(payload.collation()).isEqualTo(schema.collation());
            })
            .verifyComplete();
      }

      @Test
      @DisplayName("collation만 주어지면 charset은 스키마 값으로 상속한다")
      void inheritsSchemaCharsetWhenOnlyCollationGiven() {
        var command = TableFixture.createCommandWithMeta(" ", " utf8mb4_unicode_ci ");
        var schema = SchemaFixture.defaultSchema();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(ulidGeneratorPort.generate())
            .willReturn(TableFixture.DEFAULT_ID);
        given(createTablePort.createTable(any(Table.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createTable(command))
            .assertNext(result -> {
              var payload = result.result();
              assertThat(payload.charset()).isEqualTo(schema.charset());
              assertThat(payload.collation()).isEqualTo("utf8mb4_unicode_ci");
            })
            .verifyComplete();
      }

    }

    @Nested
    @DisplayName("스키마를 찾을 수 없으면")
    class WithMissingSchema {

      @Test
      @DisplayName("SchemaNotExistException을 발생시키고 생성을 중단한다")
      void throwsSchemaNotExistException() {
        var command = TableFixture.createCommandWithoutMeta();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.createTable(command))
            .expectErrorMatches(DomainException.hasErrorCode(SchemaErrorCode.NOT_FOUND))
            .verify();

        then(ulidGeneratorPort).shouldHaveNoInteractions();
        then(createTablePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("TableNameDuplicateException을 발생시킨다")
      void throwsTableNameDuplicateException() {
        var command = TableFixture.createCommand();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.createTable(command))
            .expectErrorMatches(DomainException.hasErrorCode(TableErrorCode.NAME_DUPLICATE))
            .verify();

        then(createTablePort).shouldHaveNoInteractions();
        then(ulidGeneratorPort).shouldHaveNoInteractions();
        then(getSchemaByIdPort).shouldHaveNoInteractions();
      }

    }

  }

}
