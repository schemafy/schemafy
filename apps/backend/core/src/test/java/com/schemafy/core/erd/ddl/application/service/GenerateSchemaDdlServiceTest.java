package com.schemafy.core.erd.ddl.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlGenerator;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.Index;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.IndexSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.Table;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot.TableSnapshot;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;
import com.schemafy.core.erd.ddl.domain.mysql.MySqlDdlGenerator;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.IdentifierLengthUnit;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenerateSchemaDdlService")
class GenerateSchemaDdlServiceTest {

  @Test
  @DisplayName("targetDbVendor에 맞는 core DDL generator로 라우팅한다")
  void routesToDdlGeneratorByTargetDbVendor() {
    DdlSchemaSnapshot snapshot = new DdlSchemaSnapshot(
        new SchemaSnapshot("schema-1", "mysql", "app", null, null),
        java.util.List.of());
    DdlGenerator mysqlGenerator = new StubDdlGenerator(
        DdlExportVendor.MYSQL, snapshot, "MYSQL DDL");
    DdlGenerator postgresGenerator = new StubDdlGenerator(
        DdlExportVendor.of("postgresql"), snapshot, "POSTGRESQL DDL");
    GenerateSchemaDdlService sut = new GenerateSchemaDdlService(
        List.of(mysqlGenerator, postgresGenerator));

    Mono<String> result = sut.generateSchemaDdl(
        new GenerateSchemaDdlCommand(
            snapshot,
            DdlExportVendor.MYSQL,
            DbVendorFixture.defaultCapabilities().indexes(),
            DbVendorFixture.defaultCapabilities().identifiers()));

    StepVerifier.create(result)
        .expectNext("MYSQL DDL")
        .verifyComplete();
  }

  @Test
  @DisplayName("지원하지 않는 targetDbVendor이면 예외가 발생한다")
  void throwsWhenTargetDbVendorIsUnsupported() {
    DdlSchemaSnapshot snapshot = new DdlSchemaSnapshot(
        new SchemaSnapshot("schema-1", "mysql", "app", null, null),
        java.util.List.of());
    GenerateSchemaDdlService sut = new GenerateSchemaDdlService(
        List.of(new StubDdlGenerator(DdlExportVendor.MYSQL, snapshot,
            "MYSQL DDL")));

    Mono<String> result = sut.generateSchemaDdl(
        new GenerateSchemaDdlCommand(snapshot,
            DdlExportVendor.of("postgresql"),
            DbVendorFixture.defaultCapabilities().indexes(),
            DbVendorFixture.defaultCapabilities().identifiers()));

    StepVerifier.create(result)
        .expectErrorMatches(DomainException.hasErrorCode(
            DdlErrorCode.UNSUPPORTED_VENDOR))
        .verify();
  }

  @Test
  @DisplayName("DDL identifier 길이를 vendor profile의 측정 단위로 검증한다")
  void validatesIdentifierLengthWithVendorMeasurementUnit() {
    DdlSchemaSnapshot snapshot = new DdlSchemaSnapshot(
        new SchemaSnapshot("schema-1", "mysql", "가나다", null, null),
        List.of());
    GenerateSchemaDdlService sut = new GenerateSchemaDdlService(
        List.of(new StubDdlGenerator(DdlExportVendor.MYSQL, snapshot, "MYSQL DDL")));

    Mono<String> result = sut.generateSchemaDdl(new GenerateSchemaDdlCommand(
        snapshot,
        DdlExportVendor.MYSQL,
        DbVendorFixture.defaultCapabilities().indexes(),
        new IdentifierCapabilities(8, IdentifierLengthUnit.UTF8_BYTES)));

    StepVerifier.create(result)
        .expectErrorSatisfies(error -> assertThat(error)
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Schema name")
            .hasMessageContaining("8 UTF-8 bytes"))
        .verify();
  }

  @Test
  @DisplayName("vendor profile이 허용하면 64자를 넘는 MySQL DDL identifier도 생성한다")
  void generatesIdentifierLongerThanLegacyMysqlConstantWhenProfileAllowsIt() {
    String schemaName = "a".repeat(65);
    DdlSchemaSnapshot snapshot = new DdlSchemaSnapshot(
        new SchemaSnapshot("schema-1", "mysql", schemaName, null, null),
        List.of());
    GenerateSchemaDdlService sut = new GenerateSchemaDdlService(
        List.of(new MySqlDdlGenerator()));

    Mono<String> result = sut.generateSchemaDdl(new GenerateSchemaDdlCommand(
        snapshot,
        DdlExportVendor.MYSQL,
        DbVendorFixture.defaultCapabilities().indexes(),
        IdentifierCapabilities.codePoints(128)));

    StepVerifier.create(result)
        .assertNext(ddl -> assertThat(ddl).contains("CREATE SCHEMA IF NOT EXISTS `" + schemaName + "`"))
        .verifyComplete();
  }

  @Test
  @DisplayName("프로젝트 벤더가 지원하지 않는 인덱스 타입이면 생성 전에 거부한다")
  void rejectsUnsupportedIndexTypeBeforeGeneration() {
    DdlSchemaSnapshot snapshot = new DdlSchemaSnapshot(
        new SchemaSnapshot("schema-1", "mysql", "app", null, null),
        List.of(new TableSnapshot(
            new Table("table-1", "schema-1", "users", null, null),
            List.of(),
            List.of(),
            List.of(),
            List.of(new IndexSnapshot(
                new Index("index-1", "table-1", "idx_users", IndexType.HASH),
                List.of())))));
    GenerateSchemaDdlService sut = new GenerateSchemaDdlService(
        List.of(new StubDdlGenerator(DdlExportVendor.MYSQL, snapshot, "MYSQL DDL")));

    Mono<String> result = sut.generateSchemaDdl(new GenerateSchemaDdlCommand(
        snapshot,
        DdlExportVendor.MYSQL,
        DbVendorFixture.defaultCapabilities().indexes(),
        DbVendorFixture.defaultCapabilities().identifiers()));

    StepVerifier.create(result)
        .expectError(DomainException.class)
        .verify();
  }

  private record StubDdlGenerator(
      DdlExportVendor exportVendor,
      DdlSchemaSnapshot expectedSnapshot,
      String ddl) implements DdlGenerator {

    @Override
    public String generate(DdlSchemaSnapshot snapshot) {
      if (snapshot != expectedSnapshot) {
        return "wrong";
      }
      return ddl;
    }

  }

}
