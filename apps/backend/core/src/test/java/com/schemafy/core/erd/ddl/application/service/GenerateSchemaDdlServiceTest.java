package com.schemafy.core.erd.ddl.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlGenerator;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Index;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.IndexSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Table;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.TableSnapshot;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("GenerateSchemaDdlService")
class GenerateSchemaDdlServiceTest {

  @Test
  @DisplayName("targetDbVendor에 맞는 core DDL generator로 라우팅한다")
  void routesToDdlGeneratorByTargetDbVendor() {
    SchemaExportSnapshot snapshot = new SchemaExportSnapshot(
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
            DbVendorFixture.defaultCapabilities().indexes()));

    StepVerifier.create(result)
        .expectNext("MYSQL DDL")
        .verifyComplete();
  }

  @Test
  @DisplayName("지원하지 않는 targetDbVendor이면 예외가 발생한다")
  void throwsWhenTargetDbVendorIsUnsupported() {
    SchemaExportSnapshot snapshot = new SchemaExportSnapshot(
        new SchemaSnapshot("schema-1", "mysql", "app", null, null),
        java.util.List.of());
    GenerateSchemaDdlService sut = new GenerateSchemaDdlService(
        List.of(new StubDdlGenerator(DdlExportVendor.MYSQL, snapshot,
            "MYSQL DDL")));

    Mono<String> result = sut.generateSchemaDdl(
        new GenerateSchemaDdlCommand(snapshot,
            DdlExportVendor.of("postgresql"),
            DbVendorFixture.defaultCapabilities().indexes()));

    StepVerifier.create(result)
        .expectErrorMatches(DomainException.hasErrorCode(
            DdlErrorCode.UNSUPPORTED_VENDOR))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 벤더가 지원하지 않는 인덱스 타입이면 생성 전에 거부한다")
  void rejectsUnsupportedIndexTypeBeforeGeneration() {
    SchemaExportSnapshot snapshot = new SchemaExportSnapshot(
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
        DbVendorFixture.defaultCapabilities().indexes()));

    StepVerifier.create(result)
        .expectError(DomainException.class)
        .verify();
  }

  private record StubDdlGenerator(
      DdlExportVendor exportVendor,
      SchemaExportSnapshot expectedSnapshot,
      String ddl) implements DdlGenerator {

    @Override
    public String generate(SchemaExportSnapshot snapshot) {
      if (snapshot != expectedSnapshot) {
        return "wrong";
      }
      return ddl;
    }

  }

}
