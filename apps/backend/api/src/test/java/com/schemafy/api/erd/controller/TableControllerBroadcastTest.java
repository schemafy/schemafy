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
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableController 브로드캐스트 단위 테스트")
class TableControllerBroadcastTest {

  private static final String TABLE_ID = "table-1";
  private static final ResolvedContext CONTEXT = new ResolvedContext(
      "project-1", "schema-1");
  private static final CommittedErdOperation OPERATION = new CommittedErdOperation(
      "op-1", null, 42L, ErdOperationDerivationKind.ORIGINAL);

  @Mock
  DeleteTableUseCase deleteTableUseCase;

  @Mock
  ObjectProvider<ErdStateSyncPublisher> publisherProvider;

  @Mock
  ErdStateSyncPublisher publisher;

  TableController sut;

  @BeforeEach
  void setUp() {
    sut = new TableController(null, null, null, null, null, null, null,
        deleteTableUseCase, null, publisherProvider);
  }

  @Test
  @DisplayName("table 삭제는 삭제 전에 context를 확보하고 ACTIVE 상태를 발행한다")
  void deleteTablePublishesActiveStateWithPreResolvedContext() {
    Set<String> affectedTableIds = Set.of(TABLE_ID);
    MutationResult<Void> result = MutationResult.<Void>of(null,
        affectedTableIds).withOperation(OPERATION);
    given(publisherProvider.getIfAvailable()).willReturn(publisher);
    given(publisher.resolveFromTableId(TABLE_ID))
        .willReturn(Mono.just(CONTEXT));
    given(deleteTableUseCase.deleteTable(new DeleteTableCommand(TABLE_ID)))
        .willReturn(Mono.just(result));
    given(publisher.publishActiveWithContext(CONTEXT, affectedTableIds,
        OPERATION)).willReturn(Mono.empty());

    StepVerifier.create(sut.deleteTable(TABLE_ID))
        .expectNextCount(1)
        .verifyComplete();

    InOrder ordered = inOrder(publisher, deleteTableUseCase);
    ordered.verify(publisher).resolveFromTableId(TABLE_ID);
    ordered.verify(deleteTableUseCase)
        .deleteTable(new DeleteTableCommand(TABLE_ID));
    ordered.verify(publisher)
        .publishActiveWithContext(CONTEXT, affectedTableIds, OPERATION);
    then(publisher).shouldHaveNoMoreInteractions();
  }

}
