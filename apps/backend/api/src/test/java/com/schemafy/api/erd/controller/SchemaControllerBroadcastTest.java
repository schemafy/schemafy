package com.schemafy.api.erd.controller;

import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.service.sync.ErdStateSyncPublisher;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster.ResolvedContext;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaController 브로드캐스트 단위 테스트")
class SchemaControllerBroadcastTest {

  private static final String SCHEMA_ID = "schema-1";
  private static final ResolvedContext CONTEXT = new ResolvedContext(
      "project-1", SCHEMA_ID);
  private static final CommittedErdOperation OPERATION = new CommittedErdOperation(
      "op-1", null, 42L, ErdOperationDerivationKind.ORIGINAL);

  @Mock
  DeleteSchemaUseCase deleteSchemaUseCase;

  @Mock
  ObjectProvider<ErdStateSyncPublisher> publisherProvider;

  @Mock
  ErdStateSyncPublisher publisher;

  SchemaController sut;

  @BeforeEach
  void setUp() {
    sut = new SchemaController(null, null, null, null,
        deleteSchemaUseCase, null, null, null, publisherProvider);
  }

  @Test
  @DisplayName("schema 삭제는 삭제 전에 context를 확보하고 DELETED 상태를 발행한다")
  void deleteSchemaPublishesDeletedStateWithPreResolvedContext() {
    MutationResult<Void> result = MutationResult.<Void>of(null, Set.of())
        .withOperation(OPERATION);
    given(publisherProvider.getIfAvailable()).willReturn(publisher);
    given(publisher.resolveFromSchemaId(SCHEMA_ID))
        .willReturn(Mono.just(CONTEXT));
    given(deleteSchemaUseCase.deleteSchema(new DeleteSchemaCommand(SCHEMA_ID)))
        .willReturn(Mono.just(result));
    given(publisher.publishDeletedWithContext(CONTEXT, Set.of(), OPERATION))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.deleteSchema(SCHEMA_ID))
        .expectNextCount(1)
        .verifyComplete();

    InOrder ordered = inOrder(publisher, deleteSchemaUseCase);
    ordered.verify(publisher).resolveFromSchemaId(SCHEMA_ID);
    ordered.verify(deleteSchemaUseCase)
        .deleteSchema(new DeleteSchemaCommand(SCHEMA_ID));
    ordered.verify(publisher)
        .publishDeletedWithContext(CONTEXT, Set.of(), OPERATION);
    then(publisher).shouldHaveNoMoreInteractions();
  }

}
