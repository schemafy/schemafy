package com.schemafy.domain.erd.schema.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.domain.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeSchemaNameService 테스트")
class ChangeSchemaNameServiceTest {

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
  private static final String NEW_NAME = "new_schema_name";

  @Mock
  ChangeSchemaNamePort changeSchemaNamePort;

  @Mock
  SchemaExistsPort schemaExistsPort;

  @InjectMocks
  ChangeSchemaNameService changeSchemaNameService;

  @Test
  @DisplayName("스키마 이름 변경 성공")
  void changeSchemaName_Success() {
    ChangeSchemaNameCommand command = new ChangeSchemaNameCommand(PROJECT_ID, SCHEMA_ID, NEW_NAME);

    given(schemaExistsPort.existsActiveByProjectIdAndName(PROJECT_ID, NEW_NAME))
        .willReturn(Mono.just(false));
    given(changeSchemaNamePort.changeSchemaName(SCHEMA_ID, NEW_NAME))
        .willReturn(Mono.empty());

    StepVerifier.create(changeSchemaNameService.changeSchemaName(command))
        .verifyComplete();

    verify(schemaExistsPort).existsActiveByProjectIdAndName(PROJECT_ID, NEW_NAME);
    verify(changeSchemaNamePort).changeSchemaName(SCHEMA_ID, NEW_NAME);
  }

  @Test
  @DisplayName("중복 이름으로 변경 시 예외 발생")
  void changeSchemaName_DuplicateName_ThrowsException() {
    ChangeSchemaNameCommand command = new ChangeSchemaNameCommand(PROJECT_ID, SCHEMA_ID, NEW_NAME);

    given(schemaExistsPort.existsActiveByProjectIdAndName(PROJECT_ID, NEW_NAME))
        .willReturn(Mono.just(true));

    StepVerifier.create(changeSchemaNameService.changeSchemaName(command))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

}
