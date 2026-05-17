package com.schemafy.core.erd.operation.application.service;

import com.schemafy.core.erd.operation.domain.ErdTouchedEntity;

record FinalizedErdMutationTarget(
    String projectId,
    String schemaId,
    ErdTouchedEntity touchedEntity) {

}
