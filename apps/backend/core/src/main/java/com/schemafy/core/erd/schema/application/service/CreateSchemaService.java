package com.schemafy.core.erd.schema.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.schema.application.port.out.ActiveProjectExistsPort;
import com.schemafy.core.erd.schema.application.port.out.CreateSchemaPort;
import com.schemafy.core.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateSchemaService implements CreateSchemaUseCase {

  private final ActiveProjectExistsPort activeProjectExistsPort;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateSchemaPort createSchemaPort;
  private final SchemaExistsPort schemaExistsPort;
  private final TransactionalOperator transactionalOperator;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<CreateSchemaResult>> createSchema(CreateSchemaCommand command) {
    return erdMutationCoordinator.coordinate(ErdOperationType.CREATE_SCHEMA, command, () -> activeProjectExistsPort
        .existsActiveProjectById(command.projectId())
        .flatMap(projectExists -> {
          if (!projectExists) {
            return Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND,
                "Project not found: " + command.projectId()));
          }

          return schemaExistsPort
              .existsActiveByProjectIdAndName(command.projectId(), command.name())
              .flatMap(exists -> {
                if (exists) {
                  return Mono.error(new DomainException(SchemaErrorCode.NAME_DUPLICATE,
                      "Schema name '%s' already exists in project".formatted(command.name())));
                }

                return Mono.fromCallable(ulidGeneratorPort::generate)
                    .flatMap(id -> {
                      Schema schema = new Schema(
                          id,
                          command.projectId(),
                          command.dbVendorName(),
                          command.name(),
                          command.charset(),
                          command.collation());

                      return createSchemaPort.createSchema(schema)
                          .map(savedSchema -> new CreateSchemaResult(
                              savedSchema.id(),
                              savedSchema.projectId(),
                              savedSchema.dbVendorName(),
                              savedSchema.name(),
                              savedSchema.charset(),
                              savedSchema.collation()))
                          .map(MutationResult::empty);
                    });
              });
        }))
        .as(transactionalOperator::transactional);
  }

}
