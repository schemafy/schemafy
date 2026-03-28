package com.schemafy.core.erd.operation.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.erd.operation.application.port.out.AppendErdOperationLogPort;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.application.port.out.IncrementSchemaCollaborationRevisionPort;
import com.schemafy.core.erd.operation.application.port.out.SaveSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class ErdOperationPersistenceAdapter implements
    FindSchemaCollaborationStatePort,
    IncrementSchemaCollaborationRevisionPort,
    SaveSchemaCollaborationStatePort,
    AppendErdOperationLogPort {

  private final SchemaCollaborationStateRepository schemaCollaborationStateRepository;
  private final SchemaCollaborationStateMapper schemaCollaborationStateMapper;
  private final ErdOperationLogRepository erdOperationLogRepository;
  private final ErdOperationLogMapper erdOperationLogMapper;

  @Override
  public Mono<SchemaCollaborationState> findBySchemaId(String schemaId) {
    return schemaCollaborationStateRepository.findById(schemaId)
        .map(schemaCollaborationStateMapper::toDomain);
  }

  @Override
  public Mono<SchemaCollaborationState> save(SchemaCollaborationState schemaCollaborationState) {
    return schemaCollaborationStateRepository.save(
            schemaCollaborationStateMapper.toEntity(schemaCollaborationState))
        .map(schemaCollaborationStateMapper::toDomain);
  }

  @Override
  public Mono<SchemaCollaborationState> increment(String schemaId) {
    return schemaCollaborationStateRepository.incrementRevision(schemaId)
        .flatMap(rowsUpdated -> {
          if (rowsUpdated != 1) {
            return Mono.error(new IllegalStateException(
                "Schema collaboration state increment failed: schemaId=" + schemaId));
          }
          return schemaCollaborationStateRepository.findById(schemaId)
              .switchIfEmpty(Mono.error(new IllegalStateException(
                  "Schema collaboration state missing after increment: schemaId=" + schemaId)));
        })
        .map(schemaCollaborationStateMapper::toDomain);
  }

  @Override
  public Mono<ErdOperationLog> append(ErdOperationLog erdOperationLog) {
    return erdOperationLogRepository.save(erdOperationLogMapper.toEntity(erdOperationLog))
        .map(erdOperationLogMapper::toDomain);
  }

}
