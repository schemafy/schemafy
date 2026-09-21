package com.schemafy.core.erd.schema.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.schema.fixture.SchemaFixture;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeSchemaNameService")
class ChangeSchemaNameServiceTest {

  @Mock
  ChangeSchemaNamePort changeSchemaNamePort;

  @Mock
  SchemaExistsPort schemaExistsPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  IdentifierCapabilityResolver identifierCapabilityResolver;

  @InjectMocks
  ChangeSchemaNameService sut;

  @BeforeEach
  void setUpIdentifierCapabilities() {
    given(identifierCapabilityResolver.resolve(any(), any()))
        .willReturn(Mono.just(DbVendorFixture.defaultCapabilities().identifiers()));
  }

  @Nested
  @DisplayName("changeSchemaName 메서드는")
  class ChangeSchemaName {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("스키마 이름을 변경한다")
      void changesSchemaName() {
        var newName = "new_schema_name";
        var command = SchemaFixture.changeNameCommand(newName);

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(schemaExistsPort.existsActiveByProjectIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(changeSchemaNamePort.changeSchemaName(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeSchemaName(command))
            .expectNextCount(1)
            .verifyComplete();

        then(schemaExistsPort).should()
            .existsActiveByProjectIdAndName(SchemaFixture.DEFAULT_PROJECT_ID, command.newName());
        then(changeSchemaNamePort).should()
            .changeSchemaName(command.schemaId(), command.newName());
      }

      @Test
      @DisplayName("DB Vendor identifier 최대 길이로 이름을 변경한다")
      void changesSchemaNameAtIdentifierLimit() {
        var command = SchemaFixture.changeNameCommand("s".repeat(64));

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(schemaExistsPort.existsActiveByProjectIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(changeSchemaNamePort.changeSchemaName(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeSchemaName(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeSchemaNamePort).should()
            .changeSchemaName(command.schemaId(), command.newName());
      }

      @Test
      @DisplayName("현재 이름과 같으면 변경 없이 성공한다")
      void succeedsWithoutChangeWhenNameIsSame() {
        var command = SchemaFixture.changeNameCommand(SchemaFixture.DEFAULT_NAME);

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));

        StepVerifier.create(sut.changeSchemaName(command))
            .expectNextMatches(result -> result.operation() == null)
            .verifyComplete();

        then(schemaExistsPort).shouldHaveNoInteractions();
        then(changeSchemaNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("DB Vendor identifier 최대 길이를 초과하면")
    class WithTooLongName {

      @Test
      @DisplayName("SchemaInvalidValueException을 발생시킨다")
      void rejectsSchemaNameOverIdentifierLimit() {
        var command = SchemaFixture.changeNameCommand("s".repeat(65));

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));

        StepVerifier.create(sut.changeSchemaName(command))
            .expectErrorMatches(DomainException.hasErrorCode(SchemaErrorCode.INVALID_VALUE))
            .verify();

        then(schemaExistsPort).shouldHaveNoInteractions();
        then(changeSchemaNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("SchemaNameDuplicateException을 발생시킨다")
      void throwsSchemaNameDuplicateException() {
        var newName = "duplicate_name";
        var command = SchemaFixture.changeNameCommand(newName);

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(schemaExistsPort.existsActiveByProjectIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.changeSchemaName(command))
            .expectErrorMatches(DomainException.hasErrorCode(SchemaErrorCode.NAME_DUPLICATE))
            .verify();

        then(changeSchemaNamePort).shouldHaveNoInteractions();
      }

    }

  }

}
