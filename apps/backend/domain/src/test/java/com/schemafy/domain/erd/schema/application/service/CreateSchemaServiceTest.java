package com.schemafy.domain.erd.schema.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.out.CreateSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNameDuplicateException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSchemaService 테스트")
class CreateSchemaServiceTest {

  private static final String ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
  private static final String DB_VENDOR = "mysql";
  private static final String NAME = "test_schema";
  private static final String CHARSET = "utf8mb4";
  private static final String COLLATION = "utf8mb4_general_ci";

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateSchemaPort createSchemaPort;

  @Mock
  SchemaExistsPort schemaExistsPort;

  @InjectMocks
  CreateSchemaService createSchemaService;

  @Test
  @DisplayName("스키마 생성 성공")
  void createSchema_Success() {
    CreateSchemaCommand command = new CreateSchemaCommand(PROJECT_ID, DB_VENDOR, NAME, CHARSET, COLLATION);
    Schema schema = new Schema(ID, PROJECT_ID, DB_VENDOR, NAME, CHARSET, COLLATION);

    given(schemaExistsPort.existsActiveByProjectIdAndName(PROJECT_ID, NAME))
        .willReturn(Mono.just(false));
    given(ulidGeneratorPort.generate()).willReturn(ID);
    given(createSchemaPort.createSchema(any(Schema.class))).willReturn(Mono.just(schema));

    StepVerifier.create(createSchemaService.createSchema(command))
        .assertNext(result -> {
          assertEquals(ID, result.id());
          assertEquals(PROJECT_ID, result.projectId());
          assertEquals(DB_VENDOR, result.dbVendorName());
          assertEquals(NAME, result.name());
          assertEquals(CHARSET, result.charset());
          assertEquals(COLLATION, result.collation());
        })
        .verifyComplete();

    verify(schemaExistsPort).existsActiveByProjectIdAndName(PROJECT_ID, NAME);
    verify(ulidGeneratorPort).generate();
    verify(createSchemaPort).createSchema(any(Schema.class));
  }

  @Test
  @DisplayName("중복 이름으로 생성 시 SchemaNameDuplicateException 발생")
  void createSchema_DuplicateName_ThrowsException() {
    CreateSchemaCommand command = new CreateSchemaCommand(PROJECT_ID, DB_VENDOR, NAME, CHARSET, COLLATION);

    given(schemaExistsPort.existsActiveByProjectIdAndName(PROJECT_ID, NAME))
        .willReturn(Mono.just(true));

    StepVerifier.create(createSchemaService.createSchema(command))
        .expectError(SchemaNameDuplicateException.class)
        .verify();
  }

  @Test
  @DisplayName("생성 결과값 검증")
  void createSchema_ReturnsCorrectResult() {
    CreateSchemaCommand command = new CreateSchemaCommand(PROJECT_ID, DB_VENDOR, NAME, null, null);
    Schema schema = new Schema(ID, PROJECT_ID, DB_VENDOR, NAME, CHARSET, COLLATION);

    given(schemaExistsPort.existsActiveByProjectIdAndName(PROJECT_ID, NAME))
        .willReturn(Mono.just(false));
    given(ulidGeneratorPort.generate()).willReturn(ID);
    given(createSchemaPort.createSchema(any(Schema.class))).willReturn(Mono.just(schema));

    StepVerifier.create(createSchemaService.createSchema(command))
        .assertNext(result -> {
          assertEquals(ID, result.id());
          assertEquals(CHARSET, result.charset());
          assertEquals(COLLATION, result.collation());
        })
        .verifyComplete();
  }

}
