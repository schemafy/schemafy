package com.schemafy.domain.erd.schema.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetSchemaService 테스트")
class GetSchemaServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
  private static final String DB_VENDOR = "mysql";
  private static final String NAME = "test_schema";
  private static final String CHARSET = "utf8mb4";
  private static final String COLLATION = "utf8mb4_general_ci";

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @InjectMocks
  GetSchemaService getSchemaService;

  @Test
  @DisplayName("스키마 조회 성공")
  void findSchemaById_Success() {
    Schema schema = new Schema(SCHEMA_ID, PROJECT_ID, DB_VENDOR, NAME, CHARSET, COLLATION);

    given(getSchemaByIdPort.findSchemaById(SCHEMA_ID))
        .willReturn(Mono.just(schema));

    StepVerifier.create(getSchemaService.findSchemaById(SCHEMA_ID))
        .assertNext(result -> {
          assertEquals(SCHEMA_ID, result.id());
          assertEquals(PROJECT_ID, result.projectId());
          assertEquals(DB_VENDOR, result.dbVendorName());
          assertEquals(NAME, result.name());
          assertEquals(CHARSET, result.charset());
          assertEquals(COLLATION, result.collation());
        })
        .verifyComplete();

    verify(getSchemaByIdPort).findSchemaById(SCHEMA_ID);
  }

  @Test
  @DisplayName("스키마가 없으면 예외 발생")
  void findSchemaById_NotFound_ThrowsException() {
    given(getSchemaByIdPort.findSchemaById(SCHEMA_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(getSchemaService.findSchemaById(SCHEMA_ID))
        .expectError(RuntimeException.class)
        .verify();
  }

}
