package com.schemafy.core.erd.operation.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexTypePort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.service.IndexCapabilityResolver;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.fixture.IndexFixture;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexTypeInverse;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeIndexTypeUndoRedoHandler")
class ChangeIndexTypeUndoRedoHandlerTest {

  @Mock
  ChangeIndexTypePort changeIndexTypePort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @Mock
  IndexCapabilityResolver indexCapabilityResolver;

  @Test
  @DisplayName("inverse 타입이 프로젝트 벤더에서 지원되지 않으면 undo/redo 변경을 거부한다")
  void rejectsUnsupportedInverseType() {
    var index = IndexFixture.btreeIndexWithId("index-1");
    var sut = new ChangeIndexTypeUndoRedoHandler(
        new JsonCodec(new ObjectMapper().findAndRegisterModules()),
        ErdMutationCoordinator.noop(),
        changeIndexTypePort,
        getIndexByIdPort,
        indexCapabilityResolver);
    given(getIndexByIdPort.findIndexById("index-1"))
        .willReturn(Mono.just(index));
    given(indexCapabilityResolver.resolve(INDEX, "index-1"))
        .willReturn(Mono.just(DbVendorFixture.defaultCapabilities().indexes()));

    StepVerifier.create(sut.applyInverse(
        new ChangeIndexTypeInverse("index-1", IndexType.HASH),
        null))
        .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.TYPE_INVALID))
        .verify();

    then(changeIndexTypePort).shouldHaveNoInteractions();
  }

}
