package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

@Component
class ErdOperationLogMapper {

  ErdOperationLogEntity toEntity(ErdOperationLog erdOperationLog) {
    return ErdOperationLogEntity.builder()
        .opId(erdOperationLog.opId())
        .projectId(erdOperationLog.projectId())
        .schemaId(erdOperationLog.schemaId())
        .opType(erdOperationLog.opType().name())
        .committedRevision(erdOperationLog.committedRevision())
        .baseSchemaRevision(erdOperationLog.baseSchemaRevision())
        .clientOperationId(erdOperationLog.clientOperationId())
        .collabSessionId(erdOperationLog.collabSessionId())
        .actorUserId(erdOperationLog.actorUserId())
        .derivationKind(erdOperationLog.derivationKind().name())
        .derivedFromOpId(erdOperationLog.derivedFromOpId())
        .lifecycleState(erdOperationLog.lifecycleState().name())
        .payloadJson(erdOperationLog.payloadJson())
        .inversePayloadJson(erdOperationLog.inversePayloadJson())
        .touchedEntitiesJson(erdOperationLog.touchedEntitiesJson())
        .affectedTableIdsJson(erdOperationLog.affectedTableIdsJson())
        .build();
  }

  ErdOperationLog toDomain(ErdOperationLogEntity entity) {
    return new ErdOperationLog(
        entity.getOpId(),
        entity.getProjectId(),
        entity.getSchemaId(),
        ErdOperationType.valueOf(entity.getOpType()),
        entity.getCommittedRevision(),
        entity.getBaseSchemaRevision(),
        entity.getClientOperationId(),
        entity.getCollabSessionId(),
        entity.getActorUserId(),
        ErdOperationDerivationKind.valueOf(entity.getDerivationKind()),
        entity.getDerivedFromOpId(),
        ErdOperationLifecycleState.valueOf(entity.getLifecycleState()),
        entity.getPayloadJson(),
        entity.getInversePayloadJson(),
        entity.getTouchedEntitiesJson(),
        entity.getAffectedTableIdsJson());
  }

}
