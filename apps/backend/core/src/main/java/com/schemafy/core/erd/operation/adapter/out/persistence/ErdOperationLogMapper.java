package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

@Component
class ErdOperationLogMapper {

  ErdOperationLogEntity toEntity(ErdOperationLog erdOperationLog) {
    var entity = new ErdOperationLogEntity();
    entity.setOpId(erdOperationLog.opId());
    entity.setProjectId(erdOperationLog.projectId());
    entity.setSchemaId(erdOperationLog.schemaId());
    entity.setOpType(erdOperationLog.opType().name());
    entity.setCommittedRevision(erdOperationLog.committedRevision());
    entity.setBaseSchemaRevision(erdOperationLog.baseSchemaRevision());
    entity.setClientOperationId(erdOperationLog.clientOperationId());
    entity.setCollabSessionId(erdOperationLog.collabSessionId());
    entity.setActorUserId(erdOperationLog.actorUserId());
    entity.setDerivationKind(erdOperationLog.derivationKind().name());
    entity.setDerivedFromOpId(erdOperationLog.derivedFromOpId());
    entity.setLifecycleState(erdOperationLog.lifecycleState().name());
    entity.setPayloadJson(erdOperationLog.payloadJson());
    entity.setInversePayloadJson(erdOperationLog.inversePayloadJson());
    entity.setTouchedEntitiesJson(erdOperationLog.touchedEntitiesJson());
    entity.setAffectedTableIdsJson(erdOperationLog.affectedTableIdsJson());
    return entity;
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
