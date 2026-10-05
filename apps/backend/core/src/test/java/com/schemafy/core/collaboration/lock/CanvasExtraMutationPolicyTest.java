package com.schemafy.core.collaboration.lock;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CanvasExtraMutationPolicyTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final CanvasEditLockService locks = mock(CanvasEditLockService.class);
  private final GetTableByIdPort tables = mock(GetTableByIdPort.class);
  private final GetSchemaByIdPort schemas = mock(GetSchemaByIdPort.class);
  private final CanvasExtraMutationPolicy policy = new CanvasExtraMutationPolicy(locks, tables,
      mock(GetRelationshipByIdPort.class), schemas, new JsonObjectMetadataConverter(new JsonCodec(mapper)));

  @Test
  void canvasRequestPreservesLatestUnrelatedFields() throws Exception {
    var requested = mapper.readTree("{\"position\":{\"x\":3,\"y\":4},\"color\":\"stale\"}");
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t",
        "{\"color\":\"latest\",\"position\":{\"x\":1}}", requested))
        .assertNext(raw -> assertThat(raw).contains("latest", "\"x\":3").doesNotContain("stale"))
        .verifyComplete();
  }

  @Test
  void ordinaryReplacementCannotEraseProtectedFields() throws Exception {
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t",
        "{\"position\":{\"x\":1}}", mapper.readTree("{\"color\":\"blue\"}")))
        .assertNext(raw -> assertThat(raw).contains("position", "blue")).verifyComplete();
  }

  @Test
  void explicitNullClearsOnlyRequestedControlPoint() throws Exception {
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.RELATIONSHIP_CONTROL_POINTS, "r",
        "{\"controlPoint1\":{\"x\":1},\"controlPoint2\":{\"x\":2},\"fkHandle\":\"left\"}",
        mapper.readTree("{\"controlPoint2\":null}")))
        .assertNext(raw -> assertThat(raw).contains("controlPoint1", "left").doesNotContain("controlPoint2"))
        .verifyComplete();
  }

  @Test
  void nonOwnerCannotSave() throws Exception {
    when(locks.isEnabled()).thenReturn(true);
    when(tables.findTableById("t")).thenReturn(Mono.just(new Table("t", "s", "name", "utf8", "collation")));
    when(schemas.findSchemaById("s")).thenReturn(Mono.just(new Schema("s", "p", "schema", "utf8", "collation")));
    when(locks.renew("p", CanvasEditLockTarget.TABLE_POSITION, "t", "other"))
        .thenReturn(Mono.just(new CanvasEditLockResult(CanvasEditLockState.REJECTED, "owner")));
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", null,
        mapper.readTree("{\"position\":{\"x\":1}}"))
        .contextWrite(ErdOperationContexts.withSessionId("other")))
        .expectError(com.schemafy.core.common.exception.DomainException.class).verify();
  }

  @Test
  void ownerAndRedisOutageAllowSaving() throws Exception {
    when(locks.isEnabled()).thenReturn(true);
    when(tables.findTableById("t")).thenReturn(Mono.just(new Table("t", "s", "name", "utf8", "collation")));
    when(schemas.findSchemaById("s")).thenReturn(Mono.just(new Schema("s", "p", "schema", "utf8", "collation")));
    when(locks.renew("p", CanvasEditLockTarget.TABLE_POSITION, "t", "owner"))
        .thenReturn(Mono.just(new CanvasEditLockResult(CanvasEditLockState.RENEWED, "owner")),
            Mono.just(new CanvasEditLockResult(CanvasEditLockState.UNAVAILABLE, null)));
    for (int attempt = 0; attempt < 2; attempt++) {
      StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", "{\"color\":\"blue\"}",
          mapper.readTree("{\"position\":{\"x\":1,\"y\":2}}"))
          .contextWrite(ErdOperationContexts.withSessionId("owner")))
          .assertNext(raw -> assertThat(raw).contains("blue", "position")).verifyComplete();
    }
  }

}
