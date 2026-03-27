package com.schemafy.core.erd.operation.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("erd_operation_log")
public class ErdOperationLogEntity implements Persistable<String> {

  @Id
  @Column("op_id")
  private String opId;

  @Column("project_id")
  private String projectId;

  @Column("schema_id")
  private String schemaId;

  @Column("op_type")
  private String opType;

  @Column("committed_revision")
  private Long committedRevision;

  @Column("base_schema_revision")
  private Long baseSchemaRevision;

  @Column("client_operation_id")
  private String clientOperationId;

  @Column("collab_session_id")
  private String collabSessionId;

  @Column("actor_user_id")
  private String actorUserId;

  @Column("derivation_kind")
  private String derivationKind;

  @Column("derived_from_op_id")
  private String derivedFromOpId;

  @Column("lifecycle_state")
  private String lifecycleState;

  @Column("payload_json")
  private String payloadJson;

  @Column("inverse_payload_json")
  private String inversePayloadJson;

  @Column("touched_entities_json")
  private String touchedEntitiesJson;

  @Column("affected_table_ids_json")
  private String affectedTableIdsJson;

  @CreatedDate
  private Instant createdAt;

  @Override
  public String getId() {
    return opId;
  }

  @Override
  public boolean isNew() {
    return createdAt == null;
  }

}
