package com.schemafy.core.collaboration.lock;

import org.springframework.data.redis.RedisConnectionFailureException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.dto.ProjectPresenceParticipant;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.project.application.port.out.ProjectPresenceReadPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CanvasExtraMutationPolicyTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final CanvasEditLockService locks = mock(CanvasEditLockService.class);
  private final GetTableByIdPort tables = mock(GetTableByIdPort.class);
  private final GetSchemaByIdPort schemas = mock(GetSchemaByIdPort.class);
  private final GetRelationshipByIdPort relationships = mock(GetRelationshipByIdPort.class);
  private final ProjectPresenceReadPort presence = mock(ProjectPresenceReadPort.class);
  private final CanvasExtraMutationPolicy policy = new CanvasExtraMutationPolicy(locks, tables,
      relationships, schemas, new JsonObjectMetadataConverter(new JsonCodec(mapper)), presence);

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
    when(presence.findSession("p", "other")).thenReturn(Mono.just(
        new ProjectPresenceParticipant("other", "user", "name", null)));
    when(locks.renew("p", CanvasEditLockTarget.TABLE_POSITION, "t", "other"))
        .thenReturn(Mono.just(new CanvasEditLockResult(CanvasEditLockState.REJECTED, "owner")));
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", null,
        mapper.readTree("{\"position\":{\"x\":1}}"))
        .contextWrite(ErdOperationContexts.withSessionId("other"))
        .contextWrite(ErdOperationContexts.withActorUserId("user")))
        .expectError(com.schemafy.core.common.exception.DomainException.class).verify();
  }

  @Test
  void ownerAndRedisOutageAllowSaving() throws Exception {
    when(locks.isEnabled()).thenReturn(true);
    when(tables.findTableById("t")).thenReturn(Mono.just(new Table("t", "s", "name", "utf8", "collation")));
    when(schemas.findSchemaById("s")).thenReturn(Mono.just(new Schema("s", "p", "schema", "utf8", "collation")));
    when(presence.findSession("p", "owner")).thenReturn(Mono.just(
        new ProjectPresenceParticipant("owner", "user", "name", null)));
    when(locks.renew("p", CanvasEditLockTarget.TABLE_POSITION, "t", "owner"))
        .thenReturn(Mono.just(new CanvasEditLockResult(CanvasEditLockState.RENEWED, "owner")),
            Mono.just(new CanvasEditLockResult(CanvasEditLockState.UNAVAILABLE, null)));
    for (int attempt = 0; attempt < 2; attempt++) {
      StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", "{\"color\":\"blue\"}",
          mapper.readTree("{\"position\":{\"x\":1,\"y\":2}}"))
          .contextWrite(ErdOperationContexts.withSessionId("owner"))
          .contextWrite(ErdOperationContexts.withActorUserId("user")))
          .assertNext(raw -> assertThat(raw).contains("blue", "position")).verifyComplete();
    }
  }

  @Test
  void spoofedOwnerSessionCannotSaveOrRenewLock() throws Exception {
    enableTableLock();
    when(presence.findSession("p", "owner")).thenReturn(Mono.just(
        new ProjectPresenceParticipant("owner", "victim", "name", null)));
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", null,
        mapper.readTree("{\"position\":{\"x\":1}}"))
        .contextWrite(ErdOperationContexts.withSessionId("owner"))
        .contextWrite(ErdOperationContexts.withActorUserId("attacker")))
        .expectError(com.schemafy.core.common.exception.DomainException.class).verify();
    verify(locks, never()).renew(anyString(), any(), anyString(), anyString());
  }

  @Test
  void missingOrExpiredSessionCannotSave() throws Exception {
    enableTableLock();
    when(presence.findSession("p", "owner")).thenReturn(Mono.empty());
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", null,
        mapper.readTree("{\"position\":{\"x\":1}}"))
        .contextWrite(ErdOperationContexts.withSessionId("owner"))
        .contextWrite(ErdOperationContexts.withActorUserId("user")))
        .expectError(com.schemafy.core.common.exception.DomainException.class).verify();
    verify(locks, never()).renew(anyString(), any(), anyString(), anyString());
    verify(presence).findSession("p", "owner");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void missingSessionOrActorCannotSave(String missing) throws Exception {
    enableTableLock();
    for (boolean missingSession : new boolean[] { true, false }) {
      StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", null,
          mapper.readTree("{\"position\":{\"x\":1}}"))
          .contextWrite(ErdOperationContexts.withSessionId(missingSession ? missing : "owner"))
          .contextWrite(ErdOperationContexts.withActorUserId(missingSession ? "user" : missing)))
          .expectError(com.schemafy.core.common.exception.DomainException.class).verify();
    }
    verifyNoInteractions(presence);
    verify(locks, never()).renew(anyString(), any(), anyString(), anyString());
  }

  @Test
  void presenceRedisOutagePreservesFallback() throws Exception {
    enableTableLock();
    when(presence.findSession("p", "owner"))
        .thenReturn(Mono.error(new RedisConnectionFailureException("offline")));
    when(locks.renew("p", CanvasEditLockTarget.TABLE_POSITION, "t", "owner"))
        .thenReturn(Mono.just(new CanvasEditLockResult(CanvasEditLockState.UNAVAILABLE, null)));
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.TABLE_POSITION, "t", null,
        mapper.readTree("{\"position\":{\"x\":1}}"))
        .contextWrite(ErdOperationContexts.withSessionId("owner"))
        .contextWrite(ErdOperationContexts.withActorUserId("user")))
        .assertNext(raw -> assertThat(raw).contains("position")).verifyComplete();
  }

  @Test
  void relationshipValidatesSessionInResourceProject() throws Exception {
    enableTableLock();
    Relationship relationship = mock(Relationship.class);
    when(relationship.fkTableId()).thenReturn("t");
    when(relationships.findRelationshipById("r")).thenReturn(Mono.just(relationship));
    when(presence.findSession("p", "owner")).thenReturn(Mono.just(
        new ProjectPresenceParticipant("owner", "victim", "name", null)));
    StepVerifier.create(policy.prepare(CanvasEditLockTarget.RELATIONSHIP_CONTROL_POINTS, "r", null,
        mapper.readTree("{\"controlPoint1\":{\"x\":1}}"))
        .contextWrite(ErdOperationContexts.withSessionId("owner"))
        .contextWrite(ErdOperationContexts.withActorUserId("attacker")))
        .expectError(com.schemafy.core.common.exception.DomainException.class).verify();
    verify(presence).findSession("p", "owner");
    verify(locks, never()).renew(anyString(), any(), anyString(), anyString());
  }

  private void enableTableLock() {
    when(locks.isEnabled()).thenReturn(true);
    when(tables.findTableById("t")).thenReturn(Mono.just(new Table("t", "s", "name", "utf8", "collation")));
    when(schemas.findSchemaById("s")).thenReturn(Mono.just(new Schema("s", "p", "schema", "utf8", "collation")));
  }

}
