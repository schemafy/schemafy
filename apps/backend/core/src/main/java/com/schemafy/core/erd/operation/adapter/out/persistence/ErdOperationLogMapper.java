package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

@Component
class ErdOperationLogMapper {

  ErdOperationLogEntity toEntity(ErdOperationLog erdOperationLog) {
    return new ErdOperationLogEntity(
        erdOperationLog.opId(),
        erdOperationLog.projectId(),
        erdOperationLog.schemaId(),
        erdOperationLog.opType().name(),
        erdOperationLog.committedRevision(),
        erdOperationLog.baseSchemaRevision(),
        erdOperationLog.clientOperationId(),
        erdOperationLog.collabSessionId(),
        erdOperationLog.actorUserId(),
        erdOperationLog.derivationKind().name(),
        erdOperationLog.derivedFromOpId(),
        erdOperationLog.lifecycleState().name(),
        erdOperationLog.payloadJson(),
        erdOperationLog.inversePayloadJson(),
        erdOperationLog.affectedTableIdsJson());
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
        entity.getAffectedTableIdsJson());
  }

}
