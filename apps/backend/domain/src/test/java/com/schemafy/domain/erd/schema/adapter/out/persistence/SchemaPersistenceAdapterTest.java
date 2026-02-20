package com.schemafy.domain.erd.schema.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.domain.erd.schema.fixture.SchemaFixture;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ SchemaPersistenceAdapter.class, SchemaMapper.class, R2dbcTestConfiguration.class })
@DisplayName("SchemaPersistenceAdapter")
class SchemaPersistenceAdapterTest {

  @Autowired
  SchemaPersistenceAdapter sut;

  @Autowired
  SchemaRepository schemaRepository;

  @BeforeEach
  void setUp() {
    schemaRepository.deleteAll().block();
  }

  @Nested
  @DisplayName("createSchema 메서드는")
  class CreateSchema {

    @Test
    @DisplayName("스키마를 저장하고 반환한다")
    void savesAndReturnsSchema() {
      var schema = SchemaFixture.defaultSchema();

      StepVerifier.create(sut.createSchema(schema))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(schema.id());
            assertThat(saved.projectId()).isEqualTo(schema.projectId());
            assertThat(saved.dbVendorName()).isEqualTo(schema.dbVendorName());
            assertThat(saved.name()).isEqualTo(schema.name());
            assertThat(saved.charset()).isEqualTo(schema.charset());
            assertThat(saved.collation()).isEqualTo(schema.collation());
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findSchemaById 메서드는")
  class FindSchemaById {

    @Test
    @DisplayName("존재하는 스키마를 반환한다")
    void returnsExistingSchema() {
      var schema = SchemaFixture.defaultSchema();
      sut.createSchema(schema).block();

      StepVerifier.create(sut.findSchemaById(schema.id()))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(schema.id());
            assertThat(found.name()).isEqualTo(schema.name());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findSchemaById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsActiveByProjectIdAndName 메서드는")
  class ExistsActiveByProjectIdAndName {

    @Test
    @DisplayName("존재하면 true를 반환한다")
    void returnsTrueWhenExists() {
      var schema = SchemaFixture.defaultSchema();
      sut.createSchema(schema).block();

      StepVerifier.create(sut.existsActiveByProjectIdAndName(
          schema.projectId(), schema.name()))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 false를 반환한다")
    void returnsFalseWhenNotExists() {
      StepVerifier.create(sut.existsActiveByProjectIdAndName(
          "non-existent-project", "non-existent-name"))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeSchemaName 메서드는")
  class ChangeSchemaName {

    @Test
    @DisplayName("스키마 이름을 변경한다")
    void changesSchemaName() {
      var schema = SchemaFixture.defaultSchema();
      sut.createSchema(schema).block();
      var newName = "updated_schema_name";

      StepVerifier.create(sut.changeSchemaName(schema.id(), newName))
          .verifyComplete();

      StepVerifier.create(sut.findSchemaById(schema.id()))
          .assertNext(found -> assertThat(found.name()).isEqualTo(newName))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 스키마면 예외를 발생시킨다")
    void throwsWhenSchemaNotExists() {
      StepVerifier.create(sut.changeSchemaName("non-existent-id", "new_name"))
          .expectErrorMatches(DomainException.hasErrorCode(SchemaErrorCode.NOT_FOUND))
          .verify();
    }

  }

  @Nested
  @DisplayName("deleteSchema 메서드는")
  class DeleteSchema {

    @Test
    @DisplayName("스키마를 삭제한다")
    void deletesSchema() {
      var schema = SchemaFixture.defaultSchema();
      sut.createSchema(schema).block();

      StepVerifier.create(sut.deleteSchema(schema.id()))
          .verifyComplete();

      StepVerifier.create(sut.findSchemaById(schema.id()))
          .verifyComplete();
    }

  }

}
